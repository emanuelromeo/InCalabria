package com.incalabria.stripe_checkout.handler;

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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

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
    public void handleGiftCardPurchase(Session session) throws IOException {
        log.info("Handling gift card purchase for session: {}", session.getId());

        GiftCard giftCard = service.saveGiftCard(dataExtractor.extractGiftCardData(session).toGiftCard());
        byte[] image = service.generateGiftCardImage(giftCard);

        String adminEmailText = String.format("""
            Code: %s
            Type: %s
            Sender: %s
            Receiver: %s
            Message: %s
            """,
                giftCard.getCode(),
                giftCard.getType(),
                giftCard.getSender(),
                giftCard.getReceiver(),
                giftCard.getMessage()
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

        sendGridEmailService.sendEmail(adminEmail, "GiftCard acquistata", adminEmailText, attachments);
        log.info("Admin gift card notification email sent");
    }
}
