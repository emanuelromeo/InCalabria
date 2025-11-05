package com.incalabria.stripe_checkout.handler;

import com.incalabria.stripe_checkout.data.giftcard.GiftCardWebhookData;
import com.incalabria.stripe_checkout.extractor.GiftCardWebhookDataExtractor;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GiftCardWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(GiftCardWebhookHandler.class);

    @Autowired
    private GiftCardWebhookDataExtractor dataExtractor;

    @Autowired
    private SendGridEmailService sendGridEmailService;

    @Value("${email.to}")
    private String adminEmail;

    /**
     * Gestisce l'acquisto di una giftcard estratta dalla sessione Stripe
     */
    public void handleGiftCardPurchase(Session session) throws IOException {
        log.info("Handling gift card purchase for session: {}", session.getId());

        GiftCardWebhookData giftCardData = dataExtractor.extractGiftCardData(session);

        // Qui inserisci la tua logica personalizzata per la gift card
        // Ad esempio salvataggi nel database, invio email di conferma, notifiche, ecc.

        String adminEmailText = String.format("""
            Nuova gift card acquistata:
            Sender: %s
            Receiver: %s
            Tipo: %s
            Messaggio: %s
            """,
                giftCardData.getSender(),
                giftCardData.getReceiver(),
                giftCardData.getType(),
                giftCardData.getMessage()
        );

        sendGridEmailService.sendEmail(adminEmail, "Nuova Gift Card Acquistata", adminEmailText);
        log.info("Admin gift card notification email sent");
    }
}
