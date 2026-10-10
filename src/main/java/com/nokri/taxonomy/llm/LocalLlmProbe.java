package com.nokri.taxonomy.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class LocalLlmProbe {
    private static final Logger log = LoggerFactory.getLogger(LocalLlmProbe.class);

    private LocalLlmProbe() {
    }

    /**
     * @param baseUrl e.g. http://127.0.0.1:8080/v1
     */
    public static boolean isReachable(String baseUrl) {
        String modelsUrl = baseUrl.endsWith("/") ? baseUrl + "models" : baseUrl + "/models";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            if (!ok) {
                log.warn("Local LLM probe {} returned HTTP {}", modelsUrl, response.statusCode());
            }
            return ok;
        } catch (Exception e) {
            log.warn("Local LLM not reachable at {} ({})", modelsUrl, e.getMessage());
            return false;
        }
    }

    /** Same as {@link #isReachable(String)} but silent on connection refused (used while waiting for startup). */
    public static boolean isReachableQuiet(String baseUrl) {
        String modelsUrl = baseUrl.endsWith("/") ? baseUrl + "models" : baseUrl + "/models";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
