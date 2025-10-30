package com.incalabria.stripe_checkout.controller;

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
    public GiftCard createGiftCard(@RequestBody GiftCard v) {
        return service.createGiftCard(v.getValue(), v.getExpiryDate());
    }

    @PostMapping("/redeem")
    public ResponseEntity<GiftCard> redeemGiftCard(@RequestParam String code) {
        return service.redeemGiftCard(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateGiftCardImage(
            @RequestParam String recipient,
            @RequestParam String giftCardId,
            @RequestParam String message,
            @RequestParam String sender,
            @RequestParam String amount) throws IOException {

        byte[] imageBytes = service.generateGiftCardImage(
                recipient, giftCardId, message, sender, amount);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=giftcard_" + giftCardId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }



}

