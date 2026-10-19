package com.edunex.backend.service.ai;

import com.edunex.backend.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thin client for Groq's OpenAI-compatible chat completions endpoint. */
@Slf4j
@Component
public class GroqClient {

    public record Message(String role, String content) {
    }

    public record Completion(String content, String finishReason) {
    }

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;
    private final int maxTokens;

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public GroqClient(@Value("${app.ai.groq-api-key:}") String apiKey,
                      @Value("${app.ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                      @Value("${app.ai.model:openai/gpt-oss-120b}") String model,
                      @Value("${app.ai.max-output-tokens:2048}") int maxTokens,
                      @Value("${app.ai.reasoning-effort:low}") String reasoningEffort,
                      @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxTokens = maxTokens;
        this.reasoningEffort = reasoningEffort == null ? "" : reasoningEffort.trim();

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();

        if (this.apiKey.isBlank()) {
            log.warn("app.ai.groq-api-key / GROQ_API_KEY is not set: the AI assistant will answer 503");
        }
    }

    @SuppressWarnings("unchecked")
    public Completion complete(List<Message> messages) {
        if (apiKey.isBlank()) {
            throw new AiUnavailableException("The AI assistant is not configured");
        }
        List<Map<String, String>> wire = new ArrayList<>();
        for (Message m : messages) {
            wire.add(Map.of("role", m.role(), "content", m.content()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", wire);
        body.put("temperature", 0.3);
        body.put("max_completion_tokens", maxTokens);
        if (!reasoningEffort.isBlank()) {
            body.put("reasoning_effort", reasoningEffort);
        }

        Map<String, Object> response;
        try {
            response = client.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientResponseException e) {
            String detail = e.getResponseBodyAsString();
            log.warn("Groq returned {}: {}", e.getStatusCode().value(),
                    detail.length() > 400 ? detail.substring(0, 400) : detail);
            if (e.getStatusCode().value() == 429) {
                throw new AiUnavailableException("The AI service is busy, please try again in a moment");
            }
            throw new AiUnavailableException("The AI service is temporarily unavailable");
        } catch (RestClientException e) {
            log.warn("Groq call failed: {}", e.getMessage());
            throw new AiUnavailableException("The AI service did not respond, please try again");
        }

        if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> first)) {
            throw new AiUnavailableException("The AI service returned an unexpected answer");
        }
        Object message = first.get("message");
        String content = message instanceof Map<?, ?> m && m.get("content") instanceof String s ? s : null;
        String finish = first.get("finish_reason") instanceof String f ? f : null;
        if (content == null || content.isBlank()) {
            log.warn("Empty completion from Groq (finish_reason={})", finish);
            throw new AiUnavailableException("The AI service returned no answer, please try again");
        }
        if ("length".equals(finish)) {
            log.warn("Groq answer was cut at max_completion_tokens={}", maxTokens);
        }
        return new Completion(content, finish);
    }
}