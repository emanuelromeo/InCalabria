package com.incalabria.stripe_checkout.repository;

import com.incalabria.stripe_checkout.entity.GiftCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GiftCardRepository extends JpaRepository<GiftCard, Long> {
    Optional<GiftCard> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsBySessionId(String sessionId);
}

