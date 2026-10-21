package com.edunex.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Brevo transactional e-mail API (POST /v3/smtp/email). */
@Slf4j
@Component
public class BrevoClient {

    public static class PermanentFailure extends RuntimeException {
        public PermanentFailure(String message) {
            super(message);
        }
    }

    public static class TransientFailure extends RuntimeException {
        public TransientFailure(String message) {
            super(message);
        }
    }

    private final RestClient client;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoClient(@Value("${app.mail.brevo-api-key:}") String apiKey,
                       @Value("${app.mail.sender-email:}") String senderEmail,
                       @Value("${app.mail.sender-name:EduNex}") String senderName) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
        this.senderName = senderName;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.client = RestClient.builder().baseUrl("https://api.brevo.com").requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !senderEmail.isBlank();
    }

    public void send(MailOutboxService.Due mail) {
        Map<String, Object> to = new LinkedHashMap<>();
        to.put("email", mail.to());
        if (mail.name() != null && !mail.name().isBlank()) {
            to.put("name", mail.name());
        }
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(to),
                "subject", mail.subject(),
                "htmlContent", mail.html());
        try {
            client.post().uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String detail = e.getResponseBodyAsString();
            String message = code + " " + (detail.length() > 300 ? detail.substring(0, 300) : detail);
            if (code == 429 || code >= 500) {
                throw new TransientFailure(message);
            }
            throw new PermanentFailure(message);
        } catch (RestClientException e) {
            throw new TransientFailure(String.valueOf(e.getMessage()));
        }
    }
}