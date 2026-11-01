package smartcharge.station;

import org.springframework.stereotype.Service;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;
import smartcharge.charging.ChargingSession;
import smartcharge.charging.ChargingSessionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class StationWaitService {

    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;
    private final ChargingRequestRepository requestRepository;
    private final ChargingSessionRepository sessionRepository;

    public StationWaitService(
            StationRepository stationRepository,
            ChargerRepository chargerRepository,
            ChargingRequestRepository requestRepository,
            ChargingSessionRepository sessionRepository) {
        this.stationRepository = stationRepository;
        this.chargerRepository = chargerRepository;
        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
    }

    public List<StationWaitEstimate> getWaitEstimates() {
        return stationRepository.findAll().stream()
                .map(station -> estimateForStation(station.getId()))
                .toList();
    }

    private StationWaitEstimate estimateForStation(Long stationId) {
        List<Charger> chargers = chargerRepository.findByStationId(stationId);
        List<Charger> occupied = chargers.stream()
                .filter(charger -> "OCCUPIED".equalsIgnoreCase(charger.getStatus()))
                .toList();

        // A free charger means no wait. If none is occupied, the reason may be
        // maintenance/unavailability, for which this system has no release time.
        if (chargers.stream().anyMatch(charger -> "AVAILABLE".equalsIgnoreCase(charger.getStatus()))) {
            return new StationWaitEstimate(stationId, 0.0);
        }
        if (occupied.isEmpty()) {
            return new StationWaitEstimate(stationId, null);
        }

        List<ChargingRequest> queued = requestRepository.findByStationIdAndStatusOrderByIdAsc(stationId, "PENDING")
                .stream()
                .filter(this::isQueueEligible)
                .sorted(Comparator.comparing(ChargingRequest::getArrivalTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        double shortestWait = Double.POSITIVE_INFINITY;
        for (Charger charger : occupied) {
            List<ChargingSession> activeSessions = sessionRepository
                    .findByChargerIdAndStatus(charger.getId(), "ACTIVE");
            if (activeSessions.isEmpty()) {
                // Occupancy without an active session has no trustworthy ETA.
                return new StationWaitEstimate(stationId, null);
            }

            double laneWait = 0;
            for (ChargingSession session : activeSessions) {
                Double remaining = remainingMinutes(session);
                if (remaining == null) return new StationWaitEstimate(stationId, null);
                laneWait = Math.max(laneWait, remaining);
            }

            for (ChargingRequest request : queued) {
                if (request.getRequestedCharger() == null
                        || !charger.getId().equals(request.getRequestedCharger().getId())) {
                    continue;
                }
                Double duration = chargingMinutes(request, charger);
                if (duration == null) return new StationWaitEstimate(stationId, null);
                laneWait += duration;
            }
            shortestWait = Math.min(shortestWait, laneWait);
        }

        return new StationWaitEstimate(stationId,
                Double.isFinite(shortestWait) ? shortestWait : null);
    }

    private boolean isQueueEligible(ChargingRequest request) {
        if (!Boolean.TRUE.equals(request.getEmergencyRequested())) return true;
        String emergencyStatus = request.getEmergencyStatus();
        return "APPROVED".equalsIgnoreCase(emergencyStatus)
                || "DECLINED".equalsIgnoreCase(emergencyStatus);
    }

    private Double remainingMinutes(ChargingSession session) {
        Double estimate = session.getRequest() == null ? null : session.getRequest().getEstimatedMinutes();
        if (estimate == null || estimate < 0 || session.getStartTime() == null) return null;
        double elapsed = Math.max(0, Duration.between(session.getStartTime(), LocalDateTime.now()).toMillis() / 60_000.0);
        return Math.max(0, estimate - elapsed);
    }

    private Double chargingMinutes(ChargingRequest request, Charger charger) {
        if (request.getVehicle() == null || request.getVehicle().getBatteryCapacity() == null
                || request.getCurrentBattery() == null || request.getTargetBattery() == null
                || charger.getPowerKw() == null || charger.getPowerKw() <= 0) {
            return null;
        }
        double energyKwh = request.getVehicle().getBatteryCapacity()
                * Math.max(0, request.getTargetBattery() - request.getCurrentBattery()) / 100.0;
        return energyKwh / charger.getPowerKw() * 60.0;
    }
}
