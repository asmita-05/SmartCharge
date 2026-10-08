package smartcharge.scheduler;

import org.springframework.web.bind.annotation.*;
import smartcharge.charging.ChargingRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@CrossOrigin("*")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/algorithm")
    public Map<String, String> getAlgorithm() {

        return Map.of(
                "algorithm",
                schedulerService.getCurrentAlgorithm()
        );
    }

    @PutMapping("/algorithm")
    public Map<String, String> setAlgorithm(
            @RequestParam String algorithm) {

        schedulerService.setAlgorithm(algorithm);

        return Map.of(
                "message", "Scheduling algorithm updated successfully",
                "algorithm", schedulerService.getCurrentAlgorithm()
        );
    }

    @GetMapping("/pending")
    public List<ChargingRequest> getScheduledRequests() {

        return schedulerService.schedulePendingRequests();
    }
}