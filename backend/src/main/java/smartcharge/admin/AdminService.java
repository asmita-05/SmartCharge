package smartcharge.admin;

import org.springframework.stereotype.Service;
import smartcharge.scheduler.SchedulerService;

@Service
public class AdminService {

    private final SchedulerService schedulerService;

    public AdminService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    public String getCurrentAlgorithm() {
        return schedulerService.getCurrentAlgorithm();
    }

    public void setAlgorithm(String algorithm) {
        schedulerService.setAlgorithm(algorithm);
    }

}
