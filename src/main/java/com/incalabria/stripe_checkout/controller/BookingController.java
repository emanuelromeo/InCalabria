package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.dto.BookingDto;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final String appDomain;

    @Autowired
    public BookingController(StripeProperties stripeProperties,
                             @Value("${app.domain}") String appDomain) {
        this.appDomain = appDomain;
        com.stripe.Stripe.apiKey = stripeProperties.getApi().getSecretKey();
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody BookingDto booking) throws StripeException {

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(booking.getExperience())
                                .build())
                        .setUnitAmount(booking.getAmountInCents())
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
        metadata.put("date", booking.getDate()); // Assicurati che date sia in formato ISO o stringa compatibile
        metadata.put("privacy", booking.getPrivacy());
        metadata.put("needs", booking.getNeeds());
        metadata.put("optionals", booking.getOptionals() != null ? String.join(", ", booking.getOptionals()) : "");

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(lineItem)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setPhoneNumberCollection(SessionCreateParams.PhoneNumberCollection.builder().setEnabled(true).build())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setPaymentIntentData(paymentIntentData)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .build();

        Session session = Session.create(params);

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @PostMapping("/capture-payment-intent/{sessionId}")
    public ResponseEntity<String> capturePaymentIntent(@PathVariable String sessionId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(getPaymentIntent(sessionId));
        paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
        return ResponseEntity.ok("PaymentIntent captured successfully.");
    }

    @PostMapping("/cancel-payment-intent/{sessionId}")
    public ResponseEntity<String> cancelPaymentIntent(@PathVariable String sessionId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(getPaymentIntent(sessionId));
        paymentIntent.cancel();
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
