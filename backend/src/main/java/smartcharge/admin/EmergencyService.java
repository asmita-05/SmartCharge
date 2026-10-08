package smartcharge.admin;

import org.springframework.stereotype.Service;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;
import smartcharge.scheduler.ChargingSchedulerService;

import java.util.List;

@Service
public class EmergencyService {

    private final ChargingRequestRepository requestRepository;
    private final ChargingSchedulerService schedulerService;

    public EmergencyService(
            ChargingRequestRepository requestRepository,
            ChargingSchedulerService schedulerService) {

        this.requestRepository = requestRepository;
        this.schedulerService = schedulerService;
    }

    public ChargingRequest requestEmergency(
            Long requestId,
            String reason) {

        ChargingRequest request =
                getRequest(requestId);

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

            throw new RuntimeException(
                    "Priority review can only be requested for a waiting request");
        }

        request.setEmergencyRequested(true);
        request.setEmergencyStatus("PENDING");
        request.setEmergencyReason(reason);

        return requestRepository.save(request);
    }

    public ChargingRequest approveEmergency(
            Long requestId) {

        ChargingRequest request =
                getRequest(requestId);

        if (!Boolean.TRUE.equals(
                request.getEmergencyRequested())) {

            throw new RuntimeException(
                    "No emergency request exists");
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())
                || !"PENDING".equalsIgnoreCase(request.getEmergencyStatus())) {
            throw new RuntimeException("Only a pending priority request can be approved");
        }

        request.setEmergencyStatus("APPROVED");
        request.setPriority("HIGH");
        ChargingRequest approved = requestRepository.save(request);
        schedulerService.processPriorityRequest(requestId);
        return approved;
    }

    public ChargingRequest declineEmergency(
            Long requestId) {

        ChargingRequest request =
                getRequest(requestId);

        if (!Boolean.TRUE.equals(request.getEmergencyRequested())
                || !"PENDING".equalsIgnoreCase(request.getEmergencyStatus())
                || !"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Only a pending priority request can be declined");
        }

        request.setEmergencyStatus("DECLINED");
        request.setPriority("NORMAL");

        return requestRepository.save(request);
    }

    public List<ChargingRequest> getPendingEmergencies() {

        return requestRepository
                .findByStatus("PENDING")
                .stream()
                .filter(request ->
                        "PENDING".equalsIgnoreCase(
                                request.getEmergencyStatus()))
                .toList();
    }

    private ChargingRequest getRequest(Long requestId) {

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Charging request not found"));
    }
}
