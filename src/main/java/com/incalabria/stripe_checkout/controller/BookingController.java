package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.service.BookingService;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/booking")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final SendGridEmailService sendGridEmailService;
    private final BookingService bookingService;

    @Autowired
    public BookingController(SendGridEmailService sendGridEmailService,
                             BookingService bookingService) {
        this.sendGridEmailService = sendGridEmailService;
        this.bookingService = bookingService;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody BookingWebhookData booking) {
        Session session;
        try {
            session = bookingService.createCheckoutSession(booking);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
        log.info("Checkout session correctly built!");
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @PostMapping("/capture-payment-intent/{sessionId}")
    public ResponseEntity<String> capturePaymentIntent(@PathVariable String sessionId) {
        PaymentIntent paymentIntent;
        Session session;

        try {
            session = bookingService.retrieveSession(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            paymentIntent = bookingService.capturePaymentIntent(sessionId);
            log.info("PaymentIntent: " + paymentIntent);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            double amount = (double) session.getAmountTotal() / 100;
            String experience = session.getMetadata().get("experience");
            try {
                String emailText = String.format("""
                        Ciao %s,
                        
                        siamo felici di confermare la tua prenotazione con InCalabria!
                        Il pagamento di %.2f€ è andato a buon fine e la tua esperienza \"%s\" è ufficialmente prenotata.
                        
                        Nel frattempo, se hai domande o desideri personalizzare la tua esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su whatsapp al numero +39 3333286692.
                        Preparati a vivere la Calabria più autentica, tra mare, natura e tradizioni locali.
                        
                        A presto,
                        Il team di InCalabria
                        """, customerName, amount, experience);
                sendGridEmailService.sendEmail(customerEmail, "Prenotazione confermata!", emailText);
                log.info("Confirmation email sent to the customer");
            } catch (IOException e) {
                log.error(e.getMessage());
                return ResponseEntity.ok("PaymentIntent confirmed, but email couldn't be sent to customer");
            }
        }
        return ResponseEntity.ok("PaymentIntent captured successfully.");
    }

    @PostMapping("/cancel-payment-intent/{sessionId}")
    public ResponseEntity<String> cancelPaymentIntent(@PathVariable String sessionId) {
        PaymentIntent paymentIntent;
        Session session;

        try {
            session = bookingService.retrieveSession(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            paymentIntent = bookingService.cancelPaymentIntent(sessionId);
            log.info("PaymentIntent: " + paymentIntent);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            String experience = session.getMetadata().get("experience");
            try {
                String emailText = String.format("""
                        Ciao %s,
                        
                        ti ringraziamo per aver scelto InCalabria e per l’interesse verso le nostre esperienze.
                        Purtroppo, in questo momento non siamo in grado di confermare la tua richiesta per l’esperienza \"%s\", a causa della mancanza di disponibilità.
                        Siamo davvero spiacenti per l’inconveniente, ma ci auguriamo di poterti accogliere presto in un’altra delle nostre attività.
                        
                        Ti invitiamo a consultare il nostro sito [www.incalabria.net](https://www.incalabria.net) per scoprire altre esperienze disponibili nelle stesse date o in periodi alternativi.
                        Per qualsiasi dubbio o richiesta, puoi scriverci su Whatsapp al numero +39 3333286692, saremo felici di aiutarti a trovare un’alternativa.
                        
                        Grazie ancora per la fiducia,
                        Il team di InCalabria
                        """, customerName, experience);
                sendGridEmailService.sendEmail(customerEmail, "Richiesta rifiutata", emailText);
                log.info("Confirmation email sent to the customer");
            } catch (IOException e) {
                log.error(e.getMessage());
                return ResponseEntity.ok("PaymentIntent cancelled, but email couldn't be sent to customer");
            }
        }

        return ResponseEntity.ok("PaymentIntent cancelled successfully.");
    }
}
