package smartcharge.charging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;

import java.time.LocalDateTime;

@Service
public class SessionService {

    private final ChargingSessionRepository sessionRepository;
    private final ChargingRequestRepository requestRepository;
    private final ChargerRepository chargerRepository;

    public SessionService(
            ChargingSessionRepository sessionRepository,
            ChargingRequestRepository requestRepository,
            ChargerRepository chargerRepository) {

        this.sessionRepository = sessionRepository;
        this.requestRepository = requestRepository;
        this.chargerRepository = chargerRepository;
    }

    @Transactional
    public ChargingSession completeSession(
            Long sessionId,
            Double energyUsed) {

        ChargingSession initialSession =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Charging session not found"));

        Charger charger = chargerRepository.findByIdForUpdate(initialSession.getCharger().getId())
                .orElseThrow(() -> new RuntimeException("Session charger not found"));
        ChargingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new RuntimeException("Charging session not found"));
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException(
                    "Only an ACTIVE session can be completed");
        }

        Double estimatedMinutes = session.getRequest().getEstimatedMinutes();
        if (estimatedMinutes == null || session.getStartTime() == null
                || java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMillis()
                < estimatedMinutes * 60_000.0) {
            throw new RuntimeException("Charging completes automatically at its estimated finish time");
        }

        LocalDateTime endTime = LocalDateTime.now();

        session.setEndTime(endTime);
        session.setStatus("COMPLETED");
        session.setEnergyUsed(energyUsed);

        if (session.getStartTime() != null) {
            long seconds =
                    java.time.Duration.between(
                            session.getStartTime(),
                            endTime
                    ).getSeconds();

            session.setActualMinutes(seconds / 60.0);
        }

        ChargingRequest request =
                session.getRequest();

        request.setStatus("COMPLETED");

        charger.setStatus("AVAILABLE");

        chargerRepository.save(charger);
        requestRepository.save(request);

        return sessionRepository.save(session);
    }
}
