package com.incalabria.stripe_checkout.service;

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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final String appDomain;

    @Autowired
    public CheckoutService(StripeProperties stripeProperties,
                           @Value("${app.domain}") String appDomain) {
        this.appDomain = appDomain;
        com.stripe.Stripe.apiKey = stripeProperties.getApi().getSecretKey();
    }

    public Session createCheckoutSession(BookingDto booking) throws StripeException {
        log.info("Booking info:\n{}", booking);
        long baseAmountInCents = (long) (booking.getAmount() * 100);
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

        String successUrl = appDomain + "/success";
        String cancelUrl = appDomain + "/";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("experience", booking.getExperience());
        metadata.put("participants", String.valueOf(booking.getParticipantsNumber()));
        metadata.put("date", booking.getDatePc() != null ? booking.getDatePc() : booking.getDateMobile());
        metadata.put("time", booking.getTime());
        metadata.put("needs", booking.getNeeds());
        metadata.put("optionals", booking.getOptionals().toString());
        metadata.put("pickup", booking.getPickup());

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(baseItem)
                .addLineItem(commissionItem)
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
