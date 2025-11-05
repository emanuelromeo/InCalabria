package com.incalabria.stripe_checkout.dto;

import java.util.List;
import java.util.stream.Collectors;

public class BookingWebhookData {
    private String sessionId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String experience;
    private String participants;
    private String date;
    private String time;
    private String pickup;
    private List<OtherRequest> others;
    private String needs;
    private double total;

    public BookingWebhookData(String sessionId, String customerName, String customerEmail, String customerPhone, String experience, String participants, String date, String time, String pickup, List<OtherRequest> others, String needs, double total) {
        this.sessionId = sessionId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.experience = experience;
        this.participants = participants;
        this.date = date;
        this.time = time;
        this.pickup = pickup;
        this.others = others;
        this.needs = needs;
        this.total = total;
    }

    public boolean hasOtherRequests() {
        return others != null && !others.isEmpty();
    }

    public boolean hasNeeds() {
        return needs != null && !needs.isEmpty();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
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

    public List<OtherRequest> getOthers() {
        return others;
    }

    public void setOthers(List<OtherRequest> others) {
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
                            .map(OtherRequest::getName)
                            .collect(Collectors.joining(", ")));

        }

        if (needs != null && !needs.isEmpty()) {
            stringBuilder.append(" | Esigenze particolari: ").append(needs);
        }

        return stringBuilder.toString();
    }
}
