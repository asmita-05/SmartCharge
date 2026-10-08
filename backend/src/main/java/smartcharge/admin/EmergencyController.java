package smartcharge.admin;

import org.springframework.web.bind.annotation.*;
import smartcharge.charging.ChargingRequest;

import java.util.List;

@RestController
@RequestMapping("/api/admin/emergencies")
@CrossOrigin("*")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(
            EmergencyService emergencyService) {

        this.emergencyService = emergencyService;
    }

    @PostMapping("/{requestId}")
    public ChargingRequest requestEmergency(
            @PathVariable Long requestId,
            @RequestParam String reason) {

        return emergencyService.requestEmergency(
                requestId,
                reason
        );
    }

    @PutMapping("/{requestId}/approve")
    public ChargingRequest approveEmergency(
            @PathVariable Long requestId) {

        return emergencyService.approveEmergency(
                requestId
        );
    }

    @PutMapping("/{requestId}/decline")
    public ChargingRequest declineEmergency(
            @PathVariable Long requestId) {

        return emergencyService.declineEmergency(
                requestId
        );
    }

    @GetMapping("/pending")
    public List<ChargingRequest> getPendingEmergencies() {

        return emergencyService.getPendingEmergencies();
    }
}