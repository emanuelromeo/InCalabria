package com.incalabria.stripe_checkout.repository;

import com.incalabria.stripe_checkout.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByExperienceDateAndReviewEmailSentFalse(LocalDate date);
}
