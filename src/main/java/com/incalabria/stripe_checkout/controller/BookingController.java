package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.dto.BookingDto;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    private final String appDomain;

    @Autowired
    public BookingController(StripeProperties stripeProperties,
                             @Value("${app.domain}") String appDomain) {
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
                                .setDescription(String.valueOf(booking))
                                .addImage(booking.getImage())
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
        PaymentIntent paymentIntent = null;
        try {
            paymentIntent = PaymentIntent.retrieve(getPaymentIntent(sessionId));
        } catch (StripeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
        try {
            paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
        } catch (StripeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
        return ResponseEntity.ok("PaymentIntent captured successfully.");
    }

    @PostMapping("/cancel-payment-intent/{sessionId}")
    public ResponseEntity<String> cancelPaymentIntent(@PathVariable String sessionId) {
        PaymentIntent paymentIntent = null;
        try {
            paymentIntent = PaymentIntent.retrieve(getPaymentIntent(sessionId));
        } catch (StripeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
        try {
            paymentIntent.cancel();
        } catch (StripeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
        return ResponseEntity.ok("PaymentIntent cancelled successfully.");
    }

//    @GetMapping("/retrieve-session/{sessionId}")
//    public ResponseEntity<Map<String, Object>> getCheckoutSession(@PathVariable String sessionId) throws StripeException {
//        Session session = Session.retrieve(sessionId);
//        Map<String, Object> result = new HashMap<>();
//        result.put("id", session.getId());
//        result.put("paymentIntent", session.getPaymentIntent());
//        result.put("metadata", session.getMetadata());
//
//        return ResponseEntity.ok(result);
//    }

    public String getPaymentIntent(String sessionId) throws StripeException {
        return Session.retrieve(sessionId).getPaymentIntent();
    }
}
