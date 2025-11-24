package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.data.giftcard.GiftCardWebhookData;
import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.enumeration.GiftCardType;
import com.incalabria.stripe_checkout.repository.GiftCardRepository;
import com.incalabria.stripe_checkout.service.GiftCardService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/giftcard")
public class GiftCardController {

    private static final Logger log = LoggerFactory.getLogger(GiftCardController.class);

    @Autowired
    private GiftCardService service;

    @Autowired
    private GiftCardRepository repository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody GiftCardWebhookData giftCard) {
        Session session;
        try {
            session = service.createCheckoutSession(giftCard);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
        log.info("Checkout session correctly built!");
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @GetMapping("/test")
    public ResponseEntity<Boolean> test(@RequestParam String session_id) {
       return ResponseEntity.ok(repository.existsBySessionId(session_id));
    }

    // TEST endpoint
    @GetMapping(value = "/generate-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateGiftCardImage(
            @RequestParam GiftCardType type,
            @RequestParam String giftCardId,
            @RequestParam String receiver,
            @RequestParam String message,
            @RequestParam String sender) throws IOException {

        GiftCard giftCard = new GiftCard(type, sender, receiver, message);
        giftCard.setCode(giftCardId);
        byte[] imageBytes = service.generateGiftCardImage(giftCard);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=giftcard_" + type.name().toLowerCase() + "_" + giftCardId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }




}

