package com.example.tickets.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Works with OpenAI and any OpenAI-compatible server (e.g. Ollama at http://localhost:11434/v1).
 * Maps failures onto the application's exception types so callers stay provider-agnostic.
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiCompatibleLlmClient(String baseUrl, String apiKey, String model, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String complete(LlmRequest request) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", request.systemPrompt()),
                            Map.of("role", "user", "content", request.userPrompt())));

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 429 || status >= 500) {
                throw new LlmUnavailableException("Provider returned HTTP " + status);
            }
            if (status >= 400) {
                throw new LlmException("Provider rejected the request with HTTP " + status);
            }

            JsonNode content = mapper.readTree(response.body()).at("/choices/0/message/content");
            if (!content.isTextual()) {
                throw new LlmException("Provider response did not contain message content");
            }
            return content.asText();
        } catch (HttpTimeoutException e) {
            throw new LlmTimeoutException("Provider request timed out");
        } catch (IOException e) {
            throw new LlmUnavailableException("Provider is unreachable: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while calling provider");
        }
    }
}
