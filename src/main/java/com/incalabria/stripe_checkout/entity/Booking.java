package com.incalabria.stripe_checkout.entity;

import com.incalabria.stripe_checkout.data.booking.Language;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sessionId;
    private String customerEmail;
    private String customerName;
    private LocalDate experienceDate;
    @Enumerated(EnumType.STRING)
    private Language language;
    private boolean reviewEmailSent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDate getExperienceDate() {
        return experienceDate;
    }

    public void setExperienceDate(LocalDate experienceDate) {
        this.experienceDate = experienceDate;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean isReviewEmailSent() {
        return reviewEmailSent;
    }

    public void setReviewEmailSent(boolean reviewEmailSent) {
        this.reviewEmailSent = reviewEmailSent;
    }
}
