package com.incalabria.stripe_checkout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final String appDomain;

    @Autowired
    public BookingService(StripeProperties stripeProperties,
                          @Value("${app.domain}") String appDomain) {
        this.appDomain = appDomain;
        com.stripe.Stripe.apiKey = stripeProperties.getApi().getSecretKey();
    }

    public Session createCheckoutSession(BookingWebhookData booking) throws StripeException {
        log.info("Booking info:\n{}", booking);
        long baseAmountInCents = (long) (booking.getTotal() * 100);
        long commissionAmount = (long) (baseAmountInCents * 0.04);

        // Riga esperienza principale
        SessionCreateParams.LineItem baseItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setUnitAmount(baseAmountInCents)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(booking.getExperience())
                                .setDescription(booking.getBookingDescription())
                                .build())
                        .build())
                .setQuantity(1L)
                .build();

        // Riga commissione aggiuntiva 4%
        SessionCreateParams.LineItem commissionItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setUnitAmount(commissionAmount)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("Commissioni e tasse")
                                .build())
                        .build())
                .setQuantity(1L)
                .build();

        SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                .build();

        String successUrl = appDomain + "/experiences/success";
        String cancelUrl = appDomain + "/";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productType", "booking");
        metadata.put("experience", booking.getExperience());
        metadata.put("participants", String.valueOf(booking.getParticipants()));
        metadata.put("date", booking.getDate());
        metadata.put("time", booking.getTime());
        metadata.put("needs", booking.getNeeds());
        metadata.put("pickup", booking.getPickup());

        if (booking.hasOtherRequests()) {
            ObjectMapper objectMapper = new ObjectMapper();
            String othersJson = null;
            try {
                othersJson = objectMapper.writeValueAsString(booking.getOthers());
            } catch (JsonProcessingException e) {
                log.error("Can't parse others: " + e.getMessage());
            }
            metadata.put("others", othersJson);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(baseItem)
                .addLineItem(commissionItem)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addCustomField(
                        SessionCreateParams.CustomField.builder()
                                .setKey("taxId")
                                .setLabel(
                                        SessionCreateParams.CustomField.Label.builder()
                                                .setType(SessionCreateParams.CustomField.Label.Type.CUSTOM)
                                                .setCustom("Codice Fiscale (o P.IVA)")
                                                .build()
                                )
                                .setType(SessionCreateParams.CustomField.Type.TEXT)
                                .build()
                )
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

        log.info("Building checkout session with additional 4% commission...");
        return Session.create(params);
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    public PaymentIntent capturePaymentIntent(String sessionId) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID " + sessionId + " successfully retrieved");

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: " + paymentIntent);

        paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
        log.info("Payment Intent successfully captured");

        return paymentIntent;
    }

    public PaymentIntent cancelPaymentIntent(String sessionId) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID " + sessionId + " successfully retrieved");

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: " + paymentIntent);

        paymentIntent.cancel();
        log.info("Payment Intent successfully cancelled");

        return paymentIntent;
    }
}
