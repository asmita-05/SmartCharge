package smartcharge.charging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import smartcharge.scheduler.ChargingSchedulerService;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ChargingLifecycleScheduler {

    private final ChargingSessionRepository sessionRepository;
    private final SessionService sessionService;
    private final ChargingRequestRepository requestRepository;
    private final ChargingSchedulerService schedulerService;

    public ChargingLifecycleScheduler(ChargingSessionRepository sessionRepository,
                                      SessionService sessionService,
                                      ChargingRequestRepository requestRepository,
                                      ChargingSchedulerService schedulerService) {
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.requestRepository = requestRepository;
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedDelay = 1000)
    public void advanceChargingQueue() {
        LocalDateTime now = LocalDateTime.now();
        for (ChargingSession session : sessionRepository.findByStatus("ACTIVE")) {
            Double estimatedMinutes = session.getRequest().getEstimatedMinutes();
            if (estimatedMinutes == null || session.getStartTime() == null) continue;
            double elapsedSeconds = Duration.between(session.getStartTime(), now).toMillis() / 1000.0;
            if (elapsedSeconds >= Math.max(0, estimatedMinutes * 60.0)) {
                ChargingRequest request = session.getRequest();
                Double capacity = request.getVehicle().getBatteryCapacity();
                double energyUsed = capacity * (request.getTargetBattery() - request.getCurrentBattery()) / 100.0;
                sessionService.completeSession(session.getId(), energyUsed);
            }
        }

        int remainingRequests = requestRepository.findByStatus("PENDING").size();
        for (int i = 0; i < remainingRequests; i++) {
            if (schedulerService.processNextRequest().isEmpty()) break;
        }
    }
}
