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
            String optionals = metadata.get("optionals");
            String needs = metadata.get("needs");
            String pickup = metadata.get("pickup");

            StringBuilder adminEmailText = new StringBuilder();
            adminEmailText.append("Session ID: ").append(sessionId).append("\n");
            adminEmailText.append("Customer name: ").append(customerName).append("\n");
            adminEmailText.append("Customer email: ").append(customerEmail).append("\n");
            adminEmailText.append("Customer phone: ").append(customerPhone).append("\n");
            adminEmailText.append("Experience: ").append(experience).append("\n");
            adminEmailText.append("Participants: ").append(participants).append("\n");
            adminEmailText.append("Date: ").append(date).append("\n");
            adminEmailText.append("Time: ").append(time).append("\n");
            adminEmailText.append("Pickup: ").append(pickup).append("\n");

            String privacyEmailText = "";
            String optionalsEmailText = "";
            String needsEmailText = "";
            String timeEmailText = time.equals("morning") ? "mattina" : time.equals("afternoon") ? "pomeriggio" : "?";


            if (optionals != null && !optionals.equals("[]")) {
                adminEmailText.append("Optionals: ").append(optionals).append("\n");
                optionalsEmailText = String.format("""
                        • Altre richieste: %s
                        """, optionals.replace("[", "").replace("]",""));
            }

            if (needs != null && !needs.isEmpty()) {
                adminEmailText.append("Needs: ").append(needs).append("\n");
                needsEmailText = String.format("""
                        • Esigenze particolari: %s
                        """, needs);
            }

            adminEmailText.append("Total: ").append(String.format("%.2f", (double) session.getAmountTotal() / 100)).append("€");

            try {
                sendGridEmailService.sendEmail(emailTo, "Pagamento autorizzato", adminEmailText.toString());
                log.info("InCalabria confirm email sent");
            } catch (IOException e) {
                log.error("InCalabria confirm email error: " + e.getMessage());
            }

            try {
                String customerEmailText = String.format("""
                    Ciao %s,
                    
                    grazie per aver scelto InCalabria!
                    Abbiamo ricevuto la tua richiesta di prenotazione e il nostro team la sta verificando.
                    
                    Dettagli della richiesta:
                    • Esperienza: %s
                    • Data: %s
                    • Orario: %s
                    • Numero di partecipanti: %s
                    %s%s%s
                    Cosa succede adesso:
                    • La tua richiesta è in fase di approvazione.
                    • Ti invieremo una mail di conferma appena l’esperienza sarà confermata e il pagamento verrà elaborato in modo sicuro.
                    • Nessun addebito verrà effettuato finché non riceverai la nostra conferma.
                    
                    Nel frattempo, se desideri modificare la data o hai domande sull’esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su Whatsapp al numero +39 3333286692.
                    Non vediamo l’ora di farti vivere la Calabria più autentica tra natura e tradizioni.
                    
                    A presto,
                    Il team di InCalabria
                    """, customerName, experience, date, timeEmailText, participants, privacyEmailText, optionalsEmailText, needsEmailText);
                sendGridEmailService.sendEmail(customerEmail, "Abbiamo ricevuto la tua richiesta di esperienza in Calabria!", customerEmailText);
                log.info("Confirm email sent to: " + customerEmail);
            } catch (IOException e) {
                log.error("Customer confirm email error: " + e.getMessage());
                try {
                    sendGridEmailService.sendEmail(emailTo, "Errore di invio email al cliente", e.getMessage());
                } catch (IOException error) {
                    log.error("InCalabria alert email error: " + error.getMessage());
                }
            }
        }

        return ResponseEntity.ok("Webhook received");
    }

}
