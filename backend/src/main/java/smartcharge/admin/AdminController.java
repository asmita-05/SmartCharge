package smartcharge.admin;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
