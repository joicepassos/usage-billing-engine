package io.github.joicepassos.billing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Minimal HTTP client for integration tests against the running server. */
public final class ApiClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String apiKey;

    public ApiClient(int port, String apiKey) {
        this.baseUrl = "http://localhost:" + port;
        this.apiKey = apiKey;
    }

    public HttpResponse<String> get(String path) throws Exception {
        return send(request(path).GET());
    }

    public HttpResponse<String> post(String path, String json) throws Exception {
        return send(request(path).POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    public HttpResponse<String> patch(String path, String json) throws Exception {
        return send(request(path).method("PATCH", HttpRequest.BodyPublishers.ofString(json)));
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");
        if (apiKey != null) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
