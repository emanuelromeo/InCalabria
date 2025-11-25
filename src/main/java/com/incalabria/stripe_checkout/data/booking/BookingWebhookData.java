package com.incalabria.stripe_checkout.data.booking;

import com.incalabria.stripe_checkout.data.Customer;

import java.util.List;
import java.util.stream.Collectors;

public class BookingWebhookData {
    private String sessionId;
    private Customer customer;
    private String experience;
    private String participants;
    private String date;
    private String time;
    private String pickup;
    private List<Others> others;
    private String needs;
    private Language language;
    private double total;
    private String code;
    private double discount;

    public BookingWebhookData(String sessionId, Customer customer, String experience, String participants, String date, String time, String pickup, List<Others> others, String needs, double total, String code, double discount, Language language) {
        this.sessionId = sessionId;
        this.customer = customer;
        this.experience = experience;
        this.participants = participants;
        this.date = date;
        this.time = time;
        this.pickup = pickup;
        this.others = others;
        this.needs = needs;
        this.total = total;
        this.code = code;
        this.discount = discount;
        this.language = language;
    }

    public boolean hasOtherRequests() {
        return others != null && !others.isEmpty();
    }

    public boolean hasNeeds() {
        return needs != null && !needs.isEmpty();
    }


    //getter and setter


    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public List<Others> getOthers() {
        return others;
    }

    public void setOthers(List<Others> others) {
        this.others = others;
    }

    public String getNeeds() {
        return needs;
    }

    public void setNeeds(String needs) {
        this.needs = needs;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getBookingDescription() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Data: ")
                .append(date)
                .append(" | Ora: ")
                .append(time.equals("morning") ? "mattina" : time.equals("afternoon") ? "pomeriggio" : "errore")
                .append(" | Numero di partecipanti: ").append(participants);

        if (hasOtherRequests()) {
            stringBuilder.append(" | Altre richieste: ")
                    .append(others.stream()
                            .map(Others::getName)
                            .collect(Collectors.joining(", ")));

        }

        if (needs != null && !needs.isEmpty()) {
            stringBuilder.append(" | Esigenze particolari: ").append(needs);
        }

        return stringBuilder.toString();
    }
}
