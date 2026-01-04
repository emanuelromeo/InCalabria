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
    private String experience;
    private Double totalAmount;    // In €
    private Double supplierAmount;    // In €
    private String customerEmail;
    private String customerName;
    private String customerNumber;
    private String supplerEmail;
    private String supplierName;
    private String supplierNumber;
    private String supplierId;
    private LocalDate experienceDate;
    @Enumerated(EnumType.STRING)
    private Language language;
    private Boolean reviewEmailSent;
    private Boolean contactEmailSent;
    private Boolean transferSent;
    private Boolean payoutSent;

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public Boolean getPayoutSent() {
        return payoutSent;
    }

    public void setPayoutSent(Boolean payoutSent) {
        this.payoutSent = payoutSent;
    }

    public Boolean getReviewEmailSent() {
        return reviewEmailSent;
    }

    public void setReviewEmailSent(Boolean reviewEmailSent) {
        this.reviewEmailSent = reviewEmailSent;
    }

    public Boolean getContactEmailSent() {
        return contactEmailSent;
    }

    public void setContactEmailSent(Boolean contactEmailSent) {
        this.contactEmailSent = contactEmailSent;
    }

    public Boolean getAmountSent() {
        return transferSent;
    }

    public void setTransferSent(Boolean transferSent) {
        this.transferSent = transferSent;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setSupplierAmount(Double supplierAmount) {
        this.supplierAmount = supplierAmount;
    }

    public boolean getTransferSent() {
        return transferSent;
    }

    public void setTransferSent(boolean transferSent) {
        this.transferSent = transferSent;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getSupplierAmount() {
        return supplierAmount;
    }

    public void setSupplierAmount(double supplierAmount) {
        this.supplierAmount = supplierAmount;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

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

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
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

    public String getSupplerEmail() {
        return supplerEmail;
    }

    public void setSupplerEmail(String supplerEmail) {
        this.supplerEmail = supplerEmail;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierNumber() {
        return supplierNumber;
    }

    public void setSupplierNumber(String supplierNumber) {
        this.supplierNumber = supplierNumber;
    }
}
