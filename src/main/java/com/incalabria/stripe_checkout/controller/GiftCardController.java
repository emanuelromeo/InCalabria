package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.data.giftcard.GiftCardWebhookData;
import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.enumeration.GiftCardType;
import com.incalabria.stripe_checkout.service.GiftCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/giftcard")
public class GiftCardController {
    @Autowired
    private GiftCardService service;

    @PostMapping("/create")
    public GiftCard createGiftCard(@RequestBody GiftCardWebhookData giftCardWebhookData) {
        return service.createGiftCard(giftCardWebhookData.toGiftCard());
    }

    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateGiftCardImage(
            @RequestParam GiftCardType type,
            @RequestParam String receiver,
            @RequestParam String giftCardId,
            @RequestParam String message,
            @RequestParam String sender) throws IOException {

        byte[] imageBytes = service.generateGiftCardImage(
                type, receiver, giftCardId, message, sender);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=giftcard_" + type.name().toLowerCase() + "_" + giftCardId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }




}

