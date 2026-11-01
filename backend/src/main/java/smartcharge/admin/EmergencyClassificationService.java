package smartcharge.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import smartcharge.charging.ChargingRequest;

import java.time.LocalDateTime;

/** Applies the binary model decision while keeping service failures in the admin-review queue. */
@Service
public class EmergencyClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyClassificationService.class);

    private final EmergencyNlpClient nlpClient;

    public EmergencyClassificationService(EmergencyNlpClient nlpClient) {
        this.nlpClient = nlpClient;
    }

    public void classify(ChargingRequest request, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new IllegalArgumentException("Emergency reason must contain 1 to 500 characters");
        }
        request.setEmergencyRequested(true);
        request.setEmergencyReason(reason.trim());
        request.setNlpClassification(null);
        request.setNlpConfidence(null);
        request.setNlpModel(null);
        request.setNlpEvaluatedAt(null);
        request.setEmergencyDecisionSource("NLP_ERROR");
        request.setEmergencyStatus("PENDING");
        request.setPriority("NORMAL");

        try {
            EmergencyNlpClient.Result result = nlpClient.classify(reason.trim());
            boolean serious = "SERIOUS".equals(result.classification());
            request.setNlpClassification(result.classification());
            request.setNlpConfidence(result.confidence());
            request.setNlpModel(result.model());
            request.setNlpEvaluatedAt(LocalDateTime.now());
            request.setEmergencyDecisionSource("NLP");
            request.setEmergencyStatus(serious ? "APPROVED" : "DECLINED");
            request.setPriority(serious ? "HIGH" : "NORMAL");
        } catch (RuntimeException exception) {
            // A model outage must not block a booking or silently turn into an approval.
            // Keep the request pending so an administrator can review it.
            request.setNlpClassification("UNAVAILABLE");
            request.setNlpEvaluatedAt(LocalDateTime.now());
            request.setEmergencyStatus("PENDING");
            request.setEmergencyDecisionSource("NLP_ERROR");
            logger.warn("Emergency NLP unavailable; request remains pending: {}", exception.getMessage());
        }
    }
}
