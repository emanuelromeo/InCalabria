package com.incalabria.stripe_checkout.service;

import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.repository.GiftCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class GiftCardService {
    @Autowired
    private GiftCardRepository repository;

    public GiftCard createVoucher(int value, LocalDate expiryDate) {
        String code;
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            code = generateRandomCode(10);
            if (!repository.existsByCode(code)) {
                GiftCard v = new GiftCard();
                v.setCode(code);
                v.setValue(value);
                v.setExpiryDate(expiryDate);
                v.setUsed(false);
                return repository.save(v);
            }
        }
        // Se non ha trovato un codice unico dopo maxAttempts, lancia eccezione
        throw new IllegalStateException("Impossibile generare un codice voucher unico");
    }


    private String generateRandomCode(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public Optional<GiftCard> redeemVoucher(String code) {
        Optional<GiftCard> voucher = repository.findByCode(code);
        if (voucher.isPresent() && !voucher.get().getUsed()) {
            voucher.get().setUsed(true);
            repository.save(voucher.get());
            return voucher;
        }
        return Optional.empty();
    }
}

