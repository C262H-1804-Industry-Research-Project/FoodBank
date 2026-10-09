package foodbank.webapp.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A stand-in for the Mapbox Search Box /forward endpoint, so the real GeocodingService
 * can be exercised over HTTP without network access or a real token.
 * Queries that were not registered get an empty result, like an unknown address.
 */
class FakeMapboxServer {

    private final HttpServer server;
    private final Map<String, double[]> coordinatesByQuery = new ConcurrentHashMap<>();
    private final Map<String, Integer> errorStatusByQuery = new ConcurrentHashMap<>();
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();

    FakeMapboxServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        server.createContext("/forward", this::handleForward);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Answers the exact query text (e.g. "16 Moseley St, Glenelg, SA") with these coordinates. */
    void respondWith(String query, double latitude, double longitude) {
        coordinatesByQuery.put(query, new double[] {latitude, longitude});
    }

    void failWith(String query, int status) {
        errorStatusByQuery.put(query, status);
    }

    /** Query parameters of every request received, oldest first. */
    List<Map<String, String>> requests() {
        return requests;
    }

    void stop() {
        server.stop(0);
    }

    void reset() {
        coordinatesByQuery.clear();
        errorStatusByQuery.clear();
        requests.clear();
    }

    private void handleForward(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        requests.add(params);
        String query = params.getOrDefault("q", "");

        Integer errorStatus = errorStatusByQuery.get(query);
        if (errorStatus != null) {
            send(exchange, errorStatus, "{\"message\":\"fake failure\"}");
            return;
        }
        double[] coordinates = coordinatesByQuery.get(query);
        // GeoJSON order is [longitude, latitude]
        String features = coordinates == null ? "" : """
                {"type": "Feature", "geometry": {"type": "Point", "coordinates": [%s, %s]},
                 "properties": {"name": "%s"}}
                """.formatted(coordinates[1], coordinates[0], query);
        send(exchange, 200, "{\"type\": \"FeatureCollection\", \"features\": [" + features + "]}");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return Map.of();
        return java.util.Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "",
                        (first, second) -> second));
    }
}
