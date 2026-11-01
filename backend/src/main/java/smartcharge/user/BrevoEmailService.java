package smartcharge.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class BrevoEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
    private final RestClient client;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService(
            @Value("${smartcharge.brevo.api-key:}") String apiKey,
            @Value("${smartcharge.brevo.sender-email:}") String senderEmail,
            @Value("${smartcharge.brevo.sender-name:SmartCharge}") String senderName) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(requestFactory)
                .build();
    }

    public void sendVerificationCode(String recipientEmail, String code) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new EmailConfigurationException();
        }

        String text = "Your SmartCharge verification code is " + code
                + ". It expires in 10 minutes. If you did not request this, ignore this email.";
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto\">"
                + "<h2>Verify your SmartCharge email</h2><p>Enter this code to finish creating your account:</p>"
                + "<p style=\"font-size:32px;font-weight:bold;letter-spacing:8px\">" + code + "</p>"
                + "<p>This code expires in 10 minutes. If you did not request it, you can ignore this email.</p></div>";

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", "Your SmartCharge verification code",
                "textContent", text,
                "htmlContent", html);

        try {
            client.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Brevo rejected or could not deliver a verification email ({}).",
                    exception.getClass().getSimpleName());
            throw new EmailDeliveryException();
        }
    }

    public static class EmailConfigurationException extends RuntimeException { }
    public static class EmailDeliveryException extends RuntimeException { }
}
