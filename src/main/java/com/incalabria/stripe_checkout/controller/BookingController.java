package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.service.BookingService;
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

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
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

    @PostMapping("/capture-payment-intent")
    public ResponseEntity<String> capturePaymentIntent(
            @RequestParam String sessionId,
            @RequestParam String connectedAccountId,
            @RequestParam Integer providerPercentage
    ) {
        Session session;

        try {
            session = bookingService.retrieveSession(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            bookingService.capturePaymentIntentAndTransferToProvider(sessionId, connectedAccountId, providerPercentage);
            log.info("PaymentIntent captured successfully");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            bookingService.sendBookingConfirmationEmail(session);
        } catch (IOException e) {
            log.error(e.getMessage());
            return ResponseEntity.ok("PaymentIntent confirmed, but email couldn't be sent to customer");
        }

        return ResponseEntity.ok("PaymentIntent captured successfully.");
    }

    @PostMapping("/cancel-payment-intent/{sessionId}")
    public ResponseEntity<String> cancelPaymentIntent(@PathVariable String sessionId) {
        Session session;

        try {
            session = bookingService.retrieveSession(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            bookingService.cancelPaymentIntent(sessionId);
            log.info("PaymentIntent cancelled successfully");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            bookingService.sendBookingCancellationEmail(session);
        } catch (IOException e) {
            log.error(e.getMessage());
            return ResponseEntity.ok("PaymentIntent cancelled, but email couldn't be sent to customer");
        }

        return ResponseEntity.ok("PaymentIntent cancelled successfully.");
    }
}
