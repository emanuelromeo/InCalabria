package com.incalabria.stripe_checkout.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final WebClient webClient;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            @Value("${brevo.api.key}") String apiKey,
            @Value("${email.from}") String senderEmail,
            @Value("${email.name}") String senderName
    ) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    public void sendEmail(String to, String subject, String body) throws IOException {
        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "email", senderEmail,
                        "name", senderName
                ),
                "to", List.of(
                        Map.of("email", to)
                ),
                "subject", subject,
                "textContent", body
        );

        String response = webClient.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    throw new RuntimeException("Errore invio email", e);
                })
                .block();

        // opzionale: puoi loggare o parsare la response JSON
    }

    public void sendEmail(String to, String subject, String body, byte[] attachmentBytes, String filename, String mimeType) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(attachmentBytes);

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "email", senderEmail,
                        "name", senderName
                ),
                "to", List.of(
                        Map.of("email", to)
                ),
                "subject", subject,
                "textContent", body,
                "attachment", List.of(
                        Map.of(
                                "name", filename,
                                "content", base64
                        )
                )
        );

        String response = webClient.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    throw new RuntimeException("Errore invio email con allegato", e);
                })
                .block();
    }

}
