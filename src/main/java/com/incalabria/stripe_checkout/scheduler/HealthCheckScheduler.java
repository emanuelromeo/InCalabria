package com.incalabria.stripe_checkout.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private static final String HEALTH_URL = "https://incalabria.onrender.com/health"; // sostituisci con il tuo URL

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 30000) // chiamata ogni 30 secondi
    public void callHealthEndpoint() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(HEALTH_URL, String.class);
        } catch (Exception e) {
            log.error("Error during health check at {}: {}", Instant.now(), e.getMessage());
        }
    }
}
