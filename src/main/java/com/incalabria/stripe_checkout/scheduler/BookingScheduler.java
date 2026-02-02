package com.incalabria.stripe_checkout.scheduler;

import com.incalabria.stripe_checkout.entity.Booking;
import com.incalabria.stripe_checkout.repository.BookingRepository;
import com.incalabria.stripe_checkout.service.BookingService;
import com.incalabria.stripe_checkout.service.EmailService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "00 00 10 * * *", zone = "Europe/Rome")
    @Transactional
    public void processBookingsNearDate() {
        LocalDate now = LocalDate.now();

        // Bookings with transfer to be sent
         List<Booking> bookings = bookingRepository
                .findByExperienceDateLessThanEqualAndTransferSentFalse(now.plusDays(3));

        for (Booking b : bookings) {
            try {
                bookingService.transferToConnectedAccount((long) (b.getSupplierAmount() * 100), b.getSupplierId());
                b.setTransferSent(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error(e.getMessage());
                emailService.sendLog("Error in transfer to account: " + b.getSupplierId(), e.getMessage());
            }
        }

        // Bookings with payout to be sent
        bookings = bookingRepository
                .findByExperienceDateLessThanEqualAndPayoutSentFalse(now.plusDays(3));

        for (Booking b : bookings) {
            try {
                bookingService.createPlatformPayout((long) ((b.getTotalAmount() - b.getSupplierAmount()) * 100));
                b.setPayoutSent(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error(e.getMessage());
                emailService.sendLog("Error in payout for session: " + b.getSessionId(), e.getMessage());
            }
        }


        // Bookings with contact email to be sent
        bookings = bookingRepository
                .findByExperienceDateLessThanEqualAndContactEmailSentFalse(now.plusDays(3));

        for (Booking b : bookings) {
            try {
                bookingService.sendContactEmailToCustomer(b);
                bookingService.sendContactEmailToSupplier(b);
                b.setContactEmailSent(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error(e.getMessage());
                emailService.sendLog("Error sending contact email", e.getMessage());
            }
        }

        // Bookings with review email to be sent
        bookings = bookingRepository
                .findByExperienceDateLessThanEqualAndReviewEmailSentFalse(now.minusDays(1));

        for (Booking b : bookings) {
            try {
                bookingService.sendReviewEmail(b);
                b.setReviewEmailSent(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error(e.getMessage());
                emailService.sendLog("Error sending review email to " + b.getCustomerEmail(), e.getMessage());
            }
        }
    }
}
