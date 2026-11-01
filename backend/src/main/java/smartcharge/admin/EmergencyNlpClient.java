package smartcharge.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Calls the local Python zero-shot classifier. The model itself never runs in the browser. */
@Component
public class EmergencyNlpClient {

    private final ObjectMapper objectMapper;
    private final URI classifyUri;
    private final Duration requestTimeout;

    public EmergencyNlpClient(
            @Value("${smartcharge.nlp.base-url:http://127.0.0.1:8090}") String baseUrl,
            @Value("${smartcharge.nlp.request-timeout-seconds:45}") long requestTimeoutSeconds) {
        // Spring Boot 4 no longer guarantees a Jackson ObjectMapper bean unless
        // the corresponding JSON auto-configuration is active. This client only
        // needs a small private mapper for its local service protocol.
        this.objectMapper = JsonMapper.builder().build();
        this.classifyUri = URI.create(baseUrl.replaceAll("/+$", "") + "/classify");
        this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
    }

    public Result classify(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Emergency reason must not be blank");
        }
        try {
            byte[] requestBody = reason.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) classifyUri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3_000);
            connection.setReadTimeout((int) Math.min(Integer.MAX_VALUE, requestTimeout.toMillis()));
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            connection.setFixedLengthStreamingMode(requestBody.length);

            try {
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBody);
                }

                int statusCode = connection.getResponseCode();
                InputStream responseStream = statusCode >= 200 && statusCode < 400
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String responseBody = responseStream == null
                        ? ""
                        : new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    throw new IllegalStateException("Local NLP service returned HTTP " + statusCode
                            + safeErrorSummary(responseBody));
                }

                JsonNode json = objectMapper.readTree(responseBody);
                String classification = json.path("classification").asText("").toUpperCase();
                double confidence = json.path("confidence").asDouble(Double.NaN);
                String model = json.path("model").asText("");
                if (!("SERIOUS".equals(classification) || "ROUTINE".equals(classification))
                        || !Double.isFinite(confidence) || confidence < 0 || confidence > 1 || model.isBlank()) {
                    throw new IllegalStateException("Local NLP service returned an invalid classification");
                }
                return new Result(classification, confidence, model);
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach the local NLP service", exception);
        }
    }

    /** Summarizes FastAPI validation errors without logging user-provided input text. */
    private String safeErrorSummary(String responseBody) {
        try {
            JsonNode details = objectMapper.readTree(responseBody).path("detail");
            if (details.isArray()) {
                StringBuilder summary = new StringBuilder();
                for (JsonNode detail : details) {
                    String location = detail.path("loc").toString();
                    String message = detail.path("msg").asText("");
                    if (!message.isBlank()) {
                        summary.append("; ").append(location).append(": ").append(message);
                    }
                }
                return summary.toString();
            }
            String detail = details.asText("");
            return detail.isBlank() ? "" : "; " + detail;
        } catch (IOException ignored) {
            return "";
        }
    }

    public record Result(String classification, double confidence, String model) {}
}
