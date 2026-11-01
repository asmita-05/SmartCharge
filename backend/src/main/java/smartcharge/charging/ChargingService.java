package smartcharge.charging;

import org.springframework.stereotype.Service;
import smartcharge.admin.EmergencyClassificationService;
import smartcharge.user.Vehicle;
import smartcharge.user.VehicleRepository;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;
import smartcharge.station.Station;
import smartcharge.station.StationRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class ChargingService {

    private final ChargingRequestRepository requestRepository;
    private final ChargingSessionRepository sessionRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargerRepository chargerRepository;
    private final StationRepository stationRepository;
    private final EmergencyClassificationService emergencyClassificationService;

    public ChargingService(
            ChargingRequestRepository requestRepository,
            ChargingSessionRepository sessionRepository,
            VehicleRepository vehicleRepository,
            ChargerRepository chargerRepository,
            StationRepository stationRepository,
            EmergencyClassificationService emergencyClassificationService) {

        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.vehicleRepository = vehicleRepository;
        this.chargerRepository = chargerRepository;
        this.stationRepository = stationRepository;
        this.emergencyClassificationService = emergencyClassificationService;
    }

    public ChargingRequest createRequest(ChargingRequest request) {

        if (request.getStation() == null || request.getStation().getId() == null
                || request.getRequestedCharger() == null || request.getRequestedCharger().getId() == null) {
            throw new RuntimeException("A charging station and charger must be selected");
        }

        if (request.getVehicle() == null
                || request.getVehicle().getId() == null) {

            throw new RuntimeException("Vehicle is required");
        }

        Vehicle vehicle = vehicleRepository.findById(
                request.getVehicle().getId()
        ).orElseThrow(() ->
                new RuntimeException("Vehicle not found"));

        if (request.getUser() == null || vehicle.getUser() == null
                || !request.getUser().getId().equals(vehicle.getUser().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only request charging for your own vehicle");
        }

        request.setVehicle(vehicle);
        if (request.getStation() != null && request.getStation().getId() != null) {
            Station station = stationRepository.findById(request.getStation().getId())
                    .orElseThrow(() -> new RuntimeException("Selected station not found"));
            request.setStation(station);
        }
        if (request.getRequestedCharger() != null && request.getRequestedCharger().getId() != null) {
            Charger charger = chargerRepository.findById(request.getRequestedCharger().getId())
                    .orElseThrow(() -> new RuntimeException("Selected charger not found"));
            if (request.getStation() == null || !charger.getStation().getId().equals(request.getStation().getId())) {
                throw new RuntimeException("Selected charger does not belong to the selected station");
            }
            request.setRequestedCharger(charger);
            request.setRequiredConnectorType(charger.getConnectorType());
        }
        request.setAdminApproved(false);
        request.setAdminDecision(null);
        request.setStatus("PAYMENT_PENDING");
        request.setPriority("NORMAL");
        request.setArrivalTime(LocalDateTime.now());
        boolean emergencyRequested = Boolean.TRUE.equals(request.getEmergencyRequested());
        request.setEmergencyDecisionSource(null);
        request.setNlpClassification(null);
        request.setNlpConfidence(null);
        request.setNlpModel(null);
        request.setNlpEvaluatedAt(null);
        request.setEmergencyStatus(emergencyRequested ? "PENDING" : "NONE");
        if (emergencyRequested) {
            String reason = request.getEmergencyReason();
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Please describe why emergency consideration is required");
            }
            if (reason.trim().length() > 500) {
                throw new IllegalArgumentException("Emergency reason must be 500 characters or fewer");
            }
            emergencyClassificationService.classify(request, reason);
        } else {
            request.setEmergencyReason(null);
        }

        validateBatteryValues(request);

        double energyKwh = vehicle.getBatteryCapacity()
                * (request.getTargetBattery() - request.getCurrentBattery()) / 100.0;
        Double chargerPower = request.getRequestedCharger().getPowerKw();
        request.setEstimatedMinutes(chargerPower == null || chargerPower <= 0
                ? null : energyKwh / chargerPower * 60.0);
        request.setEstimatedWaitMinutes(estimateWaitMinutes(request));

        return requestRepository.save(request);
    }

    private Double estimateWaitMinutes(ChargingRequest request) {
        Charger charger = request.getRequestedCharger();
        if (charger == null) return null;
        double wait = 0;
        if ("OCCUPIED".equalsIgnoreCase(charger.getStatus())) {
            List<ChargingSession> active = sessionRepository.findByChargerIdAndStatus(charger.getId(), "ACTIVE");
            if (active.isEmpty()) return null;
            for (ChargingSession session : active) {
                Double duration = session.getRequest().getEstimatedMinutes();
                if (duration == null || session.getStartTime() == null) return null;
                double elapsed = Math.max(0, Duration.between(session.getStartTime(), LocalDateTime.now()).toMillis() / 60_000.0);
                wait += Math.max(0, duration - elapsed);
            }
        } else if (!"AVAILABLE".equalsIgnoreCase(charger.getStatus())) {
            return null;
        }

        List<ChargingRequest> earlier = requestRepository.findByStationIdAndStatusOrderByIdAsc(
                        request.getStation().getId(), "PENDING").stream()
                .filter(queued -> queued.getRequestedCharger() != null
                        && charger.getId().equals(queued.getRequestedCharger().getId()))
                .filter(queued -> !Boolean.TRUE.equals(queued.getEmergencyRequested())
                        || "APPROVED".equalsIgnoreCase(queued.getEmergencyStatus())
                        || "DECLINED".equalsIgnoreCase(queued.getEmergencyStatus()))
                .toList();
        for (ChargingRequest queued : earlier) {
            Double duration = queued.getEstimatedMinutes();
            if (duration == null && queued.getVehicle() != null && queued.getVehicle().getBatteryCapacity() != null
                    && queued.getCurrentBattery() != null && queued.getTargetBattery() != null
                    && charger.getPowerKw() != null && charger.getPowerKw() > 0) {
                double energy = queued.getVehicle().getBatteryCapacity()
                        * (queued.getTargetBattery() - queued.getCurrentBattery()) / 100.0;
                duration = energy / charger.getPowerKw() * 60.0;
            }
            if (duration == null) return null;
            wait += duration;
        }
        return wait;
    }

    public List<ChargingRequest> getAllRequests() {
        return requestRepository.findAllByOrderByIdDesc();
    }

    public ChargingRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Charging request not found"));
    }

    public List<ChargingRequest> getRequestsByUser(Long userId) {
        return requestRepository.findByUserIdOrderByIdDesc(userId);
    }

    public List<ChargingRequest> getRequestsByStatus(String status) {
        return requestRepository.findByStatusOrderByIdDesc(status);
    }

    public ChargingRequest cancelRequest(Long id) {

        ChargingRequest request = getRequest(id);

        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException(
                    "Completed request cannot be cancelled");
        }

        if ("CANCELLED".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException(
                    "Request is already cancelled");
        }

        if ("ACTIVE".equalsIgnoreCase(request.getStatus())
                || "ALLOCATED".equalsIgnoreCase(request.getStatus())) {

            throw new RuntimeException(
                    "Active or allocated request cannot be cancelled");
        }

        request.setStatus("CANCELLED");

        return requestRepository.save(request);
    }

    public List<ChargingSession> getAllSessions() {
        return sessionRepository.findAllByOrderByIdDesc();
    }

    public ChargingSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Charging session not found"));
    }

    public List<ChargingSession> getSessionsByUser(Long userId) {
        return sessionRepository.findByRequestUserIdOrderByIdDesc(userId);
    }

    public List<ChargingSession> getSessionsByStatus(String status) {
        return sessionRepository.findByStatus(status);
    }

    private void validateBatteryValues(ChargingRequest request) {

        if (request.getCurrentBattery() == null
                || request.getTargetBattery() == null) {

            throw new RuntimeException(
                    "Current and target battery are required");
        }

        if (request.getCurrentBattery() < 0
                || request.getCurrentBattery() > 100) {

            throw new RuntimeException(
                    "Current battery must be between 0 and 100");
        }

        if (request.getTargetBattery() < 0
                || request.getTargetBattery() > 100) {

            throw new RuntimeException(
                    "Target battery must be between 0 and 100");
        }

        if (request.getTargetBattery()
                <= request.getCurrentBattery()) {

            throw new RuntimeException(
                    "Target battery must be greater than current battery");
        }

        if (request.getVehicle() == null) {
            throw new RuntimeException("Vehicle is required");
        }

        Vehicle vehicle = request.getVehicle();

        if (vehicle.getBatteryCapacity() == null
                || vehicle.getBatteryCapacity() <= 0) {

            throw new RuntimeException(
                    "Vehicle battery capacity must be greater than zero");
        }
    }
}
