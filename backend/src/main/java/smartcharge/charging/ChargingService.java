package smartcharge.charging;

import org.springframework.stereotype.Service;
import smartcharge.user.Vehicle;
import smartcharge.user.VehicleRepository;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;
import smartcharge.station.Station;
import smartcharge.station.StationRepository;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class ChargingService {

    private final ChargingRequestRepository requestRepository;
    private final ChargingSessionRepository sessionRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargerRepository chargerRepository;
    private final StationRepository stationRepository;

    public ChargingService(
            ChargingRequestRepository requestRepository,
            ChargingSessionRepository sessionRepository,
            VehicleRepository vehicleRepository,
            ChargerRepository chargerRepository,
            StationRepository stationRepository) {

        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.vehicleRepository = vehicleRepository;
        this.chargerRepository = chargerRepository;
        this.stationRepository = stationRepository;
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
        request.setStatus("PENDING");
        request.setPriority("NORMAL");
        request.setArrivalTime(LocalDateTime.now());
        boolean emergencyRequested = Boolean.TRUE.equals(request.getEmergencyRequested());
        request.setEmergencyStatus(emergencyRequested ? "PENDING" : "NONE");
        if (!emergencyRequested) {
            request.setEmergencyReason(null);
        }

        validateBatteryValues(request);

        // Charging duration will be calculated after
        // a compatible charger is selected.
        request.setEstimatedMinutes(null);

        return requestRepository.save(request);
    }

    public List<ChargingRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public ChargingRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Charging request not found"));
    }

    public List<ChargingRequest> getRequestsByUser(Long userId) {
        return requestRepository.findByUserId(userId);
    }

    public List<ChargingRequest> getRequestsByStatus(String status) {
        return requestRepository.findByStatus(status);
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
        return sessionRepository.findAll();
    }

    public ChargingSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Charging session not found"));
    }

    public List<ChargingSession> getSessionsByUser(Long userId) {
        return sessionRepository.findByRequestUserId(userId);
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
