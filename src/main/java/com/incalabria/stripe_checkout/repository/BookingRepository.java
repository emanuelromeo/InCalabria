package com.incalabria.stripe_checkout.repository;

import com.incalabria.stripe_checkout.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByExperienceDateLessThanEqualAndReviewEmailSentFalse(LocalDate date);
    List<Booking> findByExperienceDateLessThanEqualAndTransferSentFalse(LocalDate date);
    List<Booking> findByExperienceDateLessThanEqualAndPayoutSentFalse(LocalDate date);
    List<Booking> findByExperienceDateLessThanEqualAndContactEmailSentFalse(LocalDate date);
}
