package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.dto.BookingDto;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    private final SendGridEmailService sendGridEmailService;
    private final String appDomain;

    @Autowired
    public BookingController(SendGridEmailService sendGridEmailService,
                             StripeProperties stripeProperties,
                             @Value("${app.domain}") String appDomain) {
        this.sendGridEmailService = sendGridEmailService;
        this.appDomain = appDomain;
        com.stripe.Stripe.apiKey = stripeProperties.getApi().getSecretKey();
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody BookingDto booking) {

        log.info("Booking info:\n{}", booking);
        long amountInCents = (long) (booking.getAmount() * 100);

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(booking.getExperience())
                                .setDescription(booking.getBookingDescription())
                                .build())
                        .setUnitAmount(amountInCents)
                        .build())
                .setQuantity(1L)
                .build();

        SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                .build();

        String successUrl = appDomain + "/success";
        String cancelUrl = appDomain + "/cancel";

        Map<String, String> metadata = new HashMap<>();

        metadata.put("experience", booking.getExperience());
        metadata.put("participants", String.valueOf(booking.getParticipantsNumber()));
        metadata.put("date", booking.getDatePc() != null ? booking.getDatePc() : booking.getDateMobile());
        metadata.put("time", booking.getTime());
        metadata.put("privacy", booking.getPrivacy());
        metadata.put("needs", booking.getNeeds());
        metadata.put("optionals", booking.getOptionals().toString());

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(lineItem)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setPhoneNumberCollection(SessionCreateParams.PhoneNumberCollection.builder().setEnabled(true).build())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setPaymentIntentData(paymentIntentData)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.KLARNA)
                .build();

        Session session = null;
        try {
            log.info("Building checkout session...");
            session = Session.create(params);
        } catch (StripeException e) {
            log.error(e.getMessage());
            ResponseEntity.status(500).body(e.getMessage());
        }

        log.info("Checkout session correctly built!");
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @PostMapping("/capture-payment-intent/{sessionId}")
    public ResponseEntity<String> capturePaymentIntent(@PathVariable String sessionId) {

        Session session;
        PaymentIntent paymentIntent;
        String customerEmail;
        String customerName;

        try {
            session = Session.retrieve(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
            log.info("Payment Intent retrieved: " + paymentIntent);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
        try {
            paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
            log.info("Payment Intent successfully captured");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        if (session.getCustomerDetails() != null) {
            customerEmail = session.getCustomerDetails().getEmail();
            customerName = session.getCustomerDetails().getName();

            try {
                String emailText = """
                        Write here...
                        """;
                sendGridEmailService.sendEmail(customerEmail, "Pagamento confermato", emailText);
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

        Session session;
        PaymentIntent paymentIntent;
        String customerEmail;
        String customerName;

        try {
            session = Session.retrieve(sessionId);
            log.info("Session with ID " + sessionId + " successfully retrieved");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
            log.info("Payment Intent retrieved: " + paymentIntent);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
        try {
            paymentIntent.cancel();
            log.info("Payment Intent successfully cancelled");
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }

        if (session.getCustomerDetails() != null) {
            customerEmail = session.getCustomerDetails().getEmail();
            customerName = session.getCustomerDetails().getName();

            try {
                String emailText = """
                        Write here...
                        """;
                sendGridEmailService.sendEmail(customerEmail, "Pagamento rifiutato", emailText);
                log.info("Confirmation email sent to the customer");
            } catch (IOException e) {
                log.error(e.getMessage());
                return ResponseEntity.ok("PaymentIntent cancelled, but email couldn't be sent to customer");
            }
        }

        return ResponseEntity.ok("PaymentIntent cancelled successfully.");
    }

}
