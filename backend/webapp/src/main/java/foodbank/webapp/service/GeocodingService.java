package foodbank.webapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up pickup address coordinates with the Mapbox Search Box API /forward endpoint:
 * https://docs.mapbox.com/api/search/search-box/
 * Lookups are skipped when no token is configured, and a failed lookup never blocks saving a listing.
 */
@Service
public class GeocodingService {

    public record Coordinates(double latitude, double longitude) {}

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final String accessToken;
    private final RestClient restClient;

    public GeocodingService(@Value("${mapbox.access-token:}") String accessToken) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mapbox.com/search/searchbox/v1")
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isEnabled() {
        return !accessToken.isEmpty();
    }

    public Optional<Coordinates> geocode(String addressLine, String suburb, String region) {
        if (!isEnabled() || addressLine == null || addressLine.isBlank()) return Optional.empty();
        String query = String.join(", ", addressLine.trim(),
                suburb == null ? "" : suburb.trim(),
                region == null ? "" : region.trim());
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uri -> uri.path("/forward")
                            .queryParam("q", query)
                            .queryParam("country", "au")
                            .queryParam("limit", 1)
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .body(Map.class);
            return firstCoordinates(response);
        } catch (RuntimeException exception) {
            log.warn("Unable to geocode pickup address '{}': {}", query, exception.getMessage());
            return Optional.empty();
        }
    }

    // GeoJSON features store coordinates as [longitude, latitude]
    private Optional<Coordinates> firstCoordinates(Map<?, ?> response) {
        if (response == null || !(response.get("features") instanceof List<?> features) || features.isEmpty()) {
            return Optional.empty();
        }
        if (!(features.get(0) instanceof Map<?, ?> feature)
                || !(feature.get("geometry") instanceof Map<?, ?> geometry)
                || !(geometry.get("coordinates") instanceof List<?> coordinates)
                || coordinates.size() < 2
                || !(coordinates.get(0) instanceof Number longitude)
                || !(coordinates.get(1) instanceof Number latitude)) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(latitude.doubleValue(), longitude.doubleValue()));
    }

    /** Straight-line (great-circle) distance in kilometres. */
    public static double distanceKm(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        double latitudeDelta = Math.toRadians(toLatitude - fromLatitude);
        double longitudeDelta = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
