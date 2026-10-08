package smartcharge.scheduler;

import org.springframework.web.bind.annotation.*;
import smartcharge.charging.ChargingSession;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/charging-scheduler")
@CrossOrigin("*")
public class ChargingSchedulerController {

    private final ChargingSchedulerService chargingSchedulerService;

    public ChargingSchedulerController(
            ChargingSchedulerService chargingSchedulerService) {

        this.chargingSchedulerService = chargingSchedulerService;
    }

    @PostMapping("/process-next")
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
}