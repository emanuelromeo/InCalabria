package com.incalabria.stripe_checkout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final StripeProperties stripeProperties;
    private final String emailTo;
    private final SendGridEmailService sendGridEmailService;

    @Autowired
    public StripeWebhookController(StripeProperties stripeProperties,
                                   @Value("${email.to}") String emailTo,
                                   SendGridEmailService sendGridEmailService) {
        this.stripeProperties = stripeProperties;
        this.emailTo = emailTo;
        this.sendGridEmailService = sendGridEmailService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Stripe Webhook received");
        String endpointSecret = stripeProperties.getWebhook().getSecret();
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("Event: " + event);
        } catch (SignatureVerificationException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();

            String sessionId = session.getId();

            String customerEmail = null;
            String customerPhone = null;
            String customerName = null;

            if (session.getCustomerDetails() != null) {
                customerEmail = session.getCustomerDetails().getEmail();
                customerPhone = session.getCustomerDetails().getPhone();
                customerName = session.getCustomerDetails().getName();
            }

            Map<String, String> metadata = session.getMetadata();

            String experience = metadata.get("experience");
            String participants = metadata.get("participants");
            String date = metadata.get("date");
            String time = metadata.get("time");
            String privacy = metadata.get("privacy");
            String optionals = metadata.get("optionals");
            String needs = metadata.get("needs");

            StringBuilder emailText = new StringBuilder();
            emailText.append("Session ID: ").append(sessionId).append("\n");
            emailText.append("Customer email: ").append(customerEmail).append("\n");
            emailText.append("Customer phone: ").append(customerPhone).append("\n");
            emailText.append("Customer name: ").append(customerName).append("\n");
            emailText.append("Experience: ").append(experience).append("\n");
            emailText.append("Participants: ").append(participants).append("\n");
            emailText.append("Date: ").append(date).append("\n");
            emailText.append("Time: ").append(time).append("\n");

            if (privacy != null && !privacy.isEmpty()) {
                emailText.append("Privacy: ").append(privacy).append("\n");
            }

            if (optionals != null && !optionals.equals("[]")) {
                emailText.append("Optionals: ").append(optionals).append("\n");
            }

            if (needs != null && !needs.isEmpty()) {
                emailText.append("Needs: ").append(needs).append("\n");
            }

            emailText.append("Total: ").append(session.getAmountTotal() / 100).append("€");

            try {
                sendGridEmailService.sendEmail(emailTo, "Pagamento autorizzato", emailText.toString());
                log.info("Email successfully sent!");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        return ResponseEntity.ok("Webhook received");
    }

}
