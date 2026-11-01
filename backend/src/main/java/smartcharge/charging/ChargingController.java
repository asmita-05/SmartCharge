package smartcharge.charging;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import smartcharge.scheduler.ChargingSchedulerService;
import smartcharge.user.User;
import smartcharge.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    private final ChargingService chargingService;
    private final ChargingSchedulerService chargingSchedulerService;
    private final UserRepository userRepository;

    public ChargingController(
            ChargingService chargingService,
            ChargingSchedulerService chargingSchedulerService,
            UserRepository userRepository) {

        this.chargingService = chargingService;
        this.chargingSchedulerService = chargingSchedulerService;
        this.userRepository = userRepository;
    }

    @PostMapping("/requests")
    public ChargingRequest createRequest(
            @RequestBody ChargingRequest request,
            Authentication authentication) {

        User owner = currentUser(authentication);
        request.setUser(owner);

        return chargingService.createRequest(request);
    }

    @GetMapping("/requests")
    public List<ChargingRequest> getAllRequests() {
        return chargingService.getAllRequests();
    }

    @GetMapping("/requests/{id}")
    public ChargingRequest getRequest(
            @PathVariable Long id,
            Authentication authentication) {

        ChargingRequest request = chargingService.getRequest(id);
        requireOwnerOrStaff(request.getUser(), authentication);
        return request;
    }

    @GetMapping("/requests/user/{userId}")
    public List<ChargingRequest> getUserRequests(
            @PathVariable Long userId,
            Authentication authentication) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        requireOwnerOrStaff(user, authentication);

        return chargingService.getRequestsByUser(userId);
    }

    @GetMapping("/requests/status/{status}")
    public List<ChargingRequest> getRequestsByStatus(
            @PathVariable String status) {

        return chargingService.getRequestsByStatus(status);
    }

    @PutMapping("/requests/{id}/cancel")
    public ChargingRequest cancelRequest(
            @PathVariable Long id,
            Authentication authentication) {

        requireOwnerOrStaff(chargingService.getRequest(id).getUser(), authentication);

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
            @PathVariable Long id,
            Authentication authentication) {

        ChargingSession session = chargingService.getSession(id);
        requireOwnerOrStaff(session.getRequest().getUser(), authentication);
        return session;
    }

    @GetMapping("/sessions/user/{userId}")
    public List<ChargingSession> getUserSessions(
            @PathVariable Long userId,
            Authentication authentication) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        requireOwnerOrStaff(user, authentication);

        return chargingService.getSessionsByUser(userId);
    }

    @GetMapping("/sessions/status/{status}")
    public List<ChargingSession> getSessionsByStatus(
            @PathVariable String status) {

        return chargingService.getSessionsByStatus(status);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account no longer exists"));
    }

    private void requireOwnerOrStaff(User owner, Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_OPERATOR"));
        if (!staff && !owner.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new AccessDeniedException("You can only access your own charging records");
        }
    }
}
