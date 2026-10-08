package smartcharge.scheduler;

import org.springframework.stereotype.Service;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;

import java.util.List;

@Service
public class SchedulerService {

    private String currentAlgorithm = "FCFS";

    private final ChargingRequestRepository requestRepository;

    private final FCFS fcfs = new FCFS();
    private final Priority priority = new Priority();
    private final SJF sjf = new SJF();

    public SchedulerService(ChargingRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public List<ChargingRequest> schedulePendingRequests() {

        List<ChargingRequest> pendingRequests =
                requestRepository.findByStatus("PENDING").stream()
                        .filter(request -> !Boolean.TRUE.equals(request.getEmergencyRequested())
                                || "APPROVED".equalsIgnoreCase(request.getEmergencyStatus())
                                || "DECLINED".equalsIgnoreCase(request.getEmergencyStatus()))
                        .toList();

        return schedule(pendingRequests);
    }

    public List<ChargingRequest> schedule(
            List<ChargingRequest> requests) {

        Scheduler scheduler = getScheduler();

        return scheduler.schedule(requests);
    }

    public String getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public void setAlgorithm(String algorithm) {

        if (algorithm == null || algorithm.isBlank()) {
            throw new RuntimeException(
                    "Scheduling algorithm is required");
        }

        String selected = algorithm.toUpperCase();

        if (!selected.equals("FCFS")
                && !selected.equals("PRIORITY")
                && !selected.equals("SJF")) {

            throw new RuntimeException(
                    "Invalid algorithm. Use FCFS, PRIORITY, or SJF");
        }

        this.currentAlgorithm = selected;
    }

    private Scheduler getScheduler() {

        return switch (currentAlgorithm) {
            case "PRIORITY" -> priority;
            case "SJF" -> sjf;
            default -> fcfs;
        };
    }
}
