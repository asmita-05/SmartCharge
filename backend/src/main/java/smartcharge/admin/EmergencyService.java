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
    private final EmergencyClassificationService classificationService;

    public EmergencyService(
            ChargingRequestRepository requestRepository,
            ChargingSchedulerService schedulerService,
            EmergencyClassificationService classificationService) {

        this.requestRepository = requestRepository;
        this.schedulerService = schedulerService;
        this.classificationService = classificationService;
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

        classificationService.classify(request, reason);

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

        ensureDecisionCanBeChanged(request);

        request.setEmergencyStatus("APPROVED");
        request.setPriority("HIGH");
        request.setEmergencyDecisionSource("ADMIN");
        request.setAdminApproved(true);
        request.setAdminDecision("APPROVED");
        ChargingRequest approved = requestRepository.save(request);
        schedulerService.processPriorityRequest(requestId);
        return approved;
    }

    public ChargingRequest declineEmergency(
            Long requestId) {

        ChargingRequest request =
                getRequest(requestId);

        if (!Boolean.TRUE.equals(request.getEmergencyRequested())) {
            throw new RuntimeException("No emergency request exists");
        }
        ensureDecisionCanBeChanged(request);

        request.setEmergencyStatus("DECLINED");
        request.setPriority("NORMAL");
        request.setEmergencyDecisionSource("ADMIN");
        request.setAdminApproved(false);
        request.setAdminDecision("DECLINED");

        return requestRepository.save(request);
    }

    public List<ChargingRequest> getPendingEmergencies() {

        return requestRepository
                .findByStatusOrderByIdDesc("PENDING")
                .stream()
                .filter(request ->
                        "PENDING".equalsIgnoreCase(
                                request.getEmergencyStatus()))
                .toList();
    }

    public List<ChargingRequest> getAllEmergencies() {
        return requestRepository.findByEmergencyRequestedTrueOrderByIdDesc();
    }

    private void ensureDecisionCanBeChanged(ChargingRequest request) {
        String status = request.getStatus();
        if ("COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            throw new RuntimeException("A completed or cancelled request cannot have its emergency decision changed");
        }
    }

    private ChargingRequest getRequest(Long requestId) {

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Charging request not found"));
    }
}
