package com.incalabria.stripe_checkout.service;

import com.incalabria.stripe_checkout.handler.BookingWebhookHandler;
import com.incalabria.stripe_checkout.handler.GiftCardWebhookHandler;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    @Autowired
    private BookingWebhookHandler bookingWebhookHandler;

    @Autowired
    private GiftCardWebhookHandler giftCardWebhookHandler;

    /**
     * Elabora il completamento della sessione di checkout differenziando per prodotto
     */
    public void handleCheckoutSessionCompleted(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String productType = metadata.getOrDefault("productType", "???");

        log.info("Processing webhook for productType: {}", productType);

        try {
            if ("giftcard".equals(productType)) {
                giftCardWebhookHandler.handleGiftCardPurchase(session);
            } else if ("booking".equals(productType)) {
                bookingWebhookHandler.handleBookingPurchase(session);
            } else {
                log.warn("ProductType '{}' not supported", productType);
                throw new IllegalArgumentException("ProductType " + productType + " not supported");
            }
        } catch (Exception e) {
            log.error("Error processing webhook for productType: {}", productType, e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
}
