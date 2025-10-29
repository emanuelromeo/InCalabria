package com.incalabria.stripe_checkout.controller;

import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.service.GiftCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vouchers")
public class GiftCardController {
    @Autowired
    private GiftCardService service;

    @PostMapping("/create")
    public GiftCard createVoucher(@RequestBody GiftCard v) {
        return service.createVoucher(v.getValue(), v.getExpiryDate());
    }

    @PostMapping("/redeem")
    public ResponseEntity<GiftCard> redeemVoucher(@RequestParam String code) {
        return service.redeemVoucher(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

