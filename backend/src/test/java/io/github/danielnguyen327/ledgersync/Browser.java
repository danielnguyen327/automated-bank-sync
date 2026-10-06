package io.github.danielnguyen327.ledgersync;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Calls the running API the way the frontend does: it keeps cookies between requests, and on every
 * POST it copies the XSRF-TOKEN cookie into an X-XSRF-TOKEN header.
 */
class Browser {

    record Response(int status, String body) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final String baseUrl;

    Browser(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    Response get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    Response post(String path, String json) {
        if (!cookies.containsKey("XSRF-TOKEN")) {
            get("/api/auth/csrf");
        }
        return send(jsonPost(path, json).header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN")));
    }

    Response postWithoutCsrfToken(String path, String json) {
        return send(jsonPost(path, json));
    }

    @Nullable String cookie(String name) {
        return cookies.get(name);
    }

    private HttpRequest.Builder jsonPost(String path, String json) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(json));
    }

    private Response send(HttpRequest.Builder request) {
        if (!cookies.isEmpty()) {
            request.header("Cookie", cookies.entrySet().stream()
                .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
                .collect(joining("; ")));
        }
        try {
            HttpResponse<String> response = http.send(request.build(), BodyHandlers.ofString());
            response.headers().allValues("Set-Cookie").forEach(this::remember);
            return new Response(response.statusCode(), response.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void remember(String setCookie) {
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        boolean deleted = nameAndValue[1].isEmpty() || setCookie.contains("Max-Age=0");
        if (deleted) {
            cookies.remove(nameAndValue[0]);
        } else {
            cookies.put(nameAndValue[0], nameAndValue[1]);
        }
    }
}