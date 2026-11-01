package smartcharge.admin;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminAnalyticsService analyticsService;

    public AdminController(AdminService adminService, AdminAnalyticsService analyticsService) {
        this.adminService = adminService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public AdminAnalyticsService.AnalyticsResponse getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "days must be 7, 30, or 90");
        }
        return analyticsService.getAnalytics(days);
    }

    @GetMapping("/scheduler")
    public Map<String, String> getScheduler() {

        return Map.of(
                "algorithm",
                adminService.getCurrentAlgorithm()
        );
    }

    @PutMapping("/scheduler")
    public Map<String, String> setScheduler(
            @RequestParam String algorithm) {

        adminService.setAlgorithm(algorithm);

        return Map.of(
                "message",
                "Scheduling algorithm updated successfully",
                "algorithm",
                adminService.getCurrentAlgorithm()
        );
    }

}
