package smartcharge.charging;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin("*")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PutMapping("/{sessionId}/complete")
    public ChargingSession completeSession(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Double energyUsed) {

        return sessionService.completeSession(
                sessionId,
                energyUsed
        );
    }
}