package com.incalabria.stripe_checkout.handler;

import com.incalabria.stripe_checkout.data.Customer;
import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.extractor.GiftCardWebhookDataExtractor;
import com.incalabria.stripe_checkout.service.GiftCardService;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Component
public class GiftCardWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(GiftCardWebhookHandler.class);

    @Autowired
    private GiftCardWebhookDataExtractor dataExtractor;

    @Autowired
    private GiftCardService service;

    @Autowired
    private SendGridEmailService sendGridEmailService;

    @Value("${email.to}")
    private String adminEmail;

    /**
     * Gestisce l'acquisto di una giftcard estratta dalla sessione Stripe
     */
    @Async
    public void handleGiftCardPurchase(Session session) {
        log.info("Handling gift card purchase for session: {}", session.getId());

        if (service.existsBySession(session)) {
            return;
        }

        GiftCard extractedGiftCard = dataExtractor.extractGiftCardData(session).toGiftCard();
        extractedGiftCard.setSessionId(session.getId());
        GiftCard giftCard = service.saveGiftCard(extractedGiftCard);

        Customer customer = new Customer(session);
        byte[] image = null;
        try {
            image = service.generateGiftCardImage(giftCard);
        } catch (IOException e) {
            log.error("GiftCard image generation failed: {}", e.getMessage());
        }

        String adminEmailText = String.format("""
            Code: %s
            Type: %s
            Sender: %s
            Receiver: %s
            Message: %s
            Customer: %s
            """,
                giftCard.getCode(),
                giftCard.getType(),
                giftCard.getSender(),
                giftCard.getReceiver(),
                giftCard.getMessage(),
                customer
        );

        String customerEmailText = String.format("""
            Ciao %s,
            
            Grazie per aver scelto InCalabria per il tuo regalo speciale.
            In allegato trovi la %s digitale, pronta da inoltrare alla persona a cui vuoi donarla.
            
            La card contiene il codice univoco "%s" che potrà essere utilizzato per riscattare lo sconto di %d€ su una qualunque delle nostre esperienze: tour, degustazioni, attività outdoor, visite guidate e molto altro — tutto nel cuore della Calabria più autentica.
            
            Scopri tutte le esperienze disponibili su www.incalabria.net.
            
            Un regalo che profuma di mare, montagna e tradizione.
            Buona esperienza!
            
            Il team di InCalabria
            """,
                customer.getName(),
                giftCard.getType().getName(),
                giftCard.getCode(),
                giftCard.getType().getAmount()
        );

        // Codifica l'immagine in Base64
        String encodedImage = Base64.getEncoder().encodeToString(image);

        // Crea allegato immagine
        Attachments attachments = new Attachments();
        attachments.setContent(encodedImage);
        attachments.setType("image/png"); // o jpeg, dipende dal formato immagine
        attachments.setFilename("giftcard.png");
        attachments.setDisposition("inline"); // oppure "attachment"
        attachments.setContentId("giftcardImage");

        try {
            sendGridEmailService.sendEmail(adminEmail, giftCard.getType().getName() + " acquistata", adminEmailText, attachments);
        } catch (IOException e) {
            log.error("Error in admin email: {}", e.getMessage());
        }
        log.info("Admin gift card notification email sent");

        try {
            sendGridEmailService.sendEmail(customer.getEmail(), "La tua " + giftCard.getType().getName() + " è pronta!", customerEmailText, attachments);
        } catch (IOException e) {
            log.error("Error in customer email: {}", e.getMessage());
        }
        log.info("Customer gift card notification email sent");
    }
}
