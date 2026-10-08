package smartcharge.charging;

import org.springframework.web.bind.annotation.*;
import smartcharge.scheduler.ChargingSchedulerService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/charging")
@CrossOrigin("*")
public class ChargingController {

    private final ChargingService chargingService;
    private final ChargingSchedulerService chargingSchedulerService;

    public ChargingController(
            ChargingService chargingService,
            ChargingSchedulerService chargingSchedulerService) {

        this.chargingService = chargingService;
        this.chargingSchedulerService = chargingSchedulerService;
    }

    @PostMapping("/requests")
    public ChargingRequest createRequest(
            @RequestBody ChargingRequest request) {

        return chargingService.createRequest(request);
    }

    @GetMapping("/requests")
    public List<ChargingRequest> getAllRequests() {
        return chargingService.getAllRequests();
    }

    @GetMapping("/requests/{id}")
    public ChargingRequest getRequest(
            @PathVariable Long id) {

        return chargingService.getRequest(id);
    }

    @GetMapping("/requests/user/{userId}")
    public List<ChargingRequest> getUserRequests(
            @PathVariable Long userId) {

        return chargingService.getRequestsByUser(userId);
    }

    @GetMapping("/requests/status/{status}")
    public List<ChargingRequest> getRequestsByStatus(
            @PathVariable String status) {

        return chargingService.getRequestsByStatus(status);
    }

    @PutMapping("/requests/{id}/cancel")
    public ChargingRequest cancelRequest(
            @PathVariable Long id) {

        return chargingService.cancelRequest(id);
    }

    @PostMapping("/requests/process-next")
    public Object processNextRequest() {

        Optional<ChargingSession> session =
                chargingSchedulerService.processNextRequest();

        if (session.isEmpty()) {
            return Map.of(
                    "message",
                    "No compatible charger is currently available"
            );
        }

        return session.get();
    }

    @GetMapping("/sessions")
    public List<ChargingSession> getAllSessions() {
        return chargingService.getAllSessions();
    }

    @GetMapping("/sessions/{id}")
    public ChargingSession getSession(
            @PathVariable Long id) {

        return chargingService.getSession(id);
    }

    @GetMapping("/sessions/user/{userId}")
    public List<ChargingSession> getUserSessions(
            @PathVariable Long userId) {

        return chargingService.getSessionsByUser(userId);
    }

    @GetMapping("/sessions/status/{status}")
    public List<ChargingSession> getSessionsByStatus(
            @PathVariable String status) {

        return chargingService.getSessionsByStatus(status);
    }
}