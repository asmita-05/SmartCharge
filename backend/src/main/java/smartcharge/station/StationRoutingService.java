package smartcharge.station;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;

@Service
public class StationRoutingService {

    private final StationRepository stationRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final URI routingEndpoint;

    public StationRoutingService(
            StationRepository stationRepository,
            @Value("${smartcharge.routing.ors-base-url}") String baseUrl,
            @Value("${smartcharge.routing.ors-api-key:}") String apiKey) {
        this.stationRepository = stationRepository;
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.routingEndpoint = URI.create(normalizedBaseUrl + "/directions/driving-car/json");
    }

    public List<StationRoute> getRoutes(double originLatitude, double originLongitude) {
        validateCoordinate(originLatitude, -90, 90, "latitude");
        validateCoordinate(originLongitude, -180, 180, "longitude");
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Road routing is not configured. Set the ORS_API_KEY environment variable.");
        }

        List<Station> stations = stationRepository.findAll().stream()
                .filter(station -> validStationCoordinates(station))
                .toList();
        if (stations.isEmpty()) return List.of();

        List<StationRoute> routes = new ArrayList<>();
        for (Station station : stations) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("coordinates", List.of(
                    List.of(originLongitude, originLatitude),
                    List.of(station.getLongitude(), station.getLatitude())));
            request.put("preference", "shortest");
            request.put("instructions", false);
            request.put("geometry", false);

            JsonNode response;
            try {
                String responseBody = restClient.post()
                        .uri(routingEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, apiKey)
                        .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(String.class);
                response = parseJson(responseBody);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is5xxServerError()) {
                    return getOsrmFallback(originLatitude, originLongitude, stations);
                }
                String message = switch (exception.getStatusCode().value()) {
                    case 401, 403 -> "The routing provider rejected the API key. Check that the key is valid and enabled for directions.";
                    case 429 -> "The routing provider rate limit was reached. Please wait before requesting routes again.";
                    default -> "The routing provider could not calculate routes (HTTP "
                            + exception.getStatusCode().value() + "). Check the station coordinates and provider status.";
                };
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, exception);
            } catch (RuntimeException exception) {
                return getOsrmFallback(originLatitude, originLongitude, stations);
            }

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "The road routing service returned an empty response.");
            }
            JsonNode summary = response.path("routes").path(0).path("summary");
            JsonNode distance = summary.path("distance");
            JsonNode duration = summary.path("duration");
            routes.add(new StationRoute(
                    station.getId(),
                    distance.isNumber() ? distance.doubleValue() : null,
                    duration.isNumber() ? duration.doubleValue() : null,
                    "OpenRouteService"));
        }
        return routes;
    }

    /** Uses OSRM's public table endpoint if ORS is unreachable. This keeps local demos usable. */
    private List<StationRoute> getOsrmFallback(double originLatitude, double originLongitude,
                                               List<Station> stations) {
        StringBuilder url = new StringBuilder("https://router.project-osrm.org/table/v1/driving/")
                .append(originLongitude).append(',').append(originLatitude);
        for (Station station : stations) {
            url.append(';').append(station.getLongitude()).append(',').append(station.getLatitude());
        }
        url.append("?sources=0&destinations=");
        for (int i = 0; i < stations.size(); i++) {
            if (i > 0) url.append(';');
            url.append(i + 1);
        }
        url.append("&annotations=distance,duration");

        try {
            String responseBody = restClient.get().uri(URI.create(url.toString()))
                    .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                    .retrieve().body(String.class);
            JsonNode table = parseJson(responseBody);
            if (table == null || !"Ok".equals(table.path("code").asText())) {
                throw new IllegalStateException("OSRM returned no usable route table.");
            }
            JsonNode distances = table.path("distances").path(0);
            JsonNode durations = table.path("durations").path(0);
            List<StationRoute> routes = new ArrayList<>();
            for (int i = 0; i < stations.size(); i++) {
                JsonNode distance = distances.path(i);
                JsonNode duration = durations.path(i);
                routes.add(new StationRoute(stations.get(i).getId(),
                        distance.isNumber() ? distance.doubleValue() : null,
                        duration.isNumber() ? duration.doubleValue() : null,
                        "OSRM fallback (fastest route)"));
            }
            return routes;
        } catch (RuntimeException exception) {
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            String reason = root.getClass().getSimpleName();
            if (root.getMessage() != null && !root.getMessage().isBlank()) {
                String message = root.getMessage().replaceAll("[\\r\\n]+", " ").trim();
                if (message.length() > 160) message = message.substring(0, 160) + "…";
                reason += ": " + message;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "OpenRouteService and the OSRM fallback are both unreachable (" + reason
                            + "). Check that the backend computer has internet and DNS access.", exception);
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Routing provider returned invalid JSON.", exception);
        }
    }

    private boolean validStationCoordinates(Station station) {
        return station.getLatitude() != null && station.getLongitude() != null
                && station.getLatitude() >= -90 && station.getLatitude() <= 90
                && station.getLongitude() >= -180 && station.getLongitude() <= 180;
    }

    private void validateCoordinate(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Origin " + name + " must be between " + min + " and " + max + ".");
        }
    }

    public record StationRoute(Long stationId, Double distanceMeters, Double durationSeconds, String provider) { }
}
