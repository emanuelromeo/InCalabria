package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private StripeWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Stripe Webhook received");

        Event event;
        try {
            String endpointSecret = stripeProperties.getWebhook().getSecret();
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("Event type: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            webhookService.handleCheckoutSessionCompleted(session);
        }

        return ResponseEntity.ok("Webhook received");
    }
}
