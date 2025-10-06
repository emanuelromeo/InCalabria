package com.incalabria.stripe_checkout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.config.StripeProperties;
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

import java.util.Map;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final StripeProperties stripeProperties;
    private final JavaMailSender emailSender;
    private final String emailTo;

    @Autowired
    public StripeWebhookController(StripeProperties stripeProperties,
                                   JavaMailSender emailSender,
                                   @Value("${email.to}") String emailTo) {
        this.stripeProperties = stripeProperties;
        this.emailSender = emailSender;
        this.emailTo = emailTo;
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

            // Estrai dati da customer_details
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
            String privacy = metadata.get("privacy");
            String optionals = metadata.get("optionals");
            String needs = metadata.get("needs");

            StringBuilder emailText = new StringBuilder();
            emailText.append("Session ID: ").append(sessionId).append("\n");
            emailText.append("Customer email: " ).append(customerEmail).append("\n");
            emailText.append("Customer phone: " ).append(customerPhone).append("\n");
            emailText.append("Customer name: ").append(customerName).append("\n");
            emailText.append("Experience: ").append(experience).append("\n");
            emailText.append("Participants: ").append(participants).append("\n");
            emailText.append("Date: ").append(date).append("\n");
            emailText.append("Privacy: ").append(privacy).append("\n");
            emailText.append("Optionals: ").append(optionals).append("\n");
            emailText.append("Needs: ").append(needs).append("\n");
            emailText.append("Total: ").append(session.getAmountTotal() / 100);

            log.info("Sending email...");
            sendNotificationEmail(emailTo, emailText.toString());
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void sendNotificationEmail(String to, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Pagamento autorizzato");
        message.setText(text);
        emailSender.send(message);
    }
}
