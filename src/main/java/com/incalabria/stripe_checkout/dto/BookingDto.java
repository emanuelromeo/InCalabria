package com.incalabria.stripe_checkout.dto;

import java.util.ArrayList;
import java.util.List;

public class BookingDto {

    private String experience;
    private Integer participantsNumber;
    private String datePc;
    private String dateMobile;
    private String time;
    private String privacy;
    private Boolean guide = false;
    private Boolean equipment = false;
    private Boolean transport = false;
    private Boolean insurance = false;
    private Boolean lunch = false;
    private Boolean breakfast = false;
    private String needs;
    private Double amount;
    private String image;

    public BookingDto() {
    }


    // Metodi getter e setter


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public Integer getParticipantsNumber() {
        return participantsNumber;
    }

    public void setParticipantsNumber(Integer participantsNumber) {
        this.participantsNumber = participantsNumber;
    }

    public String getDatePc() {
        return datePc;
    }

    public void setDatePc(String datePc) {
        this.datePc = datePc;
    }

    public String getDateMobile() {
        return dateMobile;
    }

    public void setDateMobile(String dateMobile) {
        this.dateMobile = dateMobile;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public Boolean getGuide() {
        return guide;
    }

    public void setGuide(Boolean guide) {
        this.guide = guide;
    }

    public Boolean getEquipment() {
        return equipment;
    }

    public void setEquipment(Boolean equipment) {
        this.equipment = equipment;
    }

    public Boolean getTransport() {
        return transport;
    }

    public void setTransport(Boolean transport) {
        this.transport = transport;
    }

    public Boolean getInsurance() {
        return insurance;
    }

    public void setInsurance(Boolean insurance) {
        this.insurance = insurance;
    }

    public Boolean getLunch() {
        return lunch;
    }

    public void setLunch(Boolean lunch) {
        this.lunch = lunch;
    }

    public Boolean getBreakfast() {
        return breakfast;
    }

    public void setBreakfast(Boolean breakfast) {
        this.breakfast = breakfast;
    }

    public String getNeeds() {
        return needs;
    }

    public void setNeeds(String needs) {
        this.needs = needs;
    }

    public List<String> getOptionals() {
        List<String> optionals = new ArrayList<>();
        if (guide) {
            optionals.add("guida");
        }
        if (equipment) {
            optionals.add("attrezzatura");
        }
        if (transport) {
            optionals.add("trasporto");
        }
        if (insurance) {
            optionals.add("assicurazione");
        }
        if (lunch) {
            optionals.add("pranzo/cena");
        }
        if (breakfast) {
            optionals.add("colazione");
        }

        return optionals;
    }

    public String getBookingDescription() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Data: ");
        stringBuilder.append(datePc != null ? datePc : dateMobile);
        stringBuilder.append(" | Ora: ").append(time.equals("morning") ? "mattina" : time.equals("afternoon") ? "pomeriggio" : "errore");
        stringBuilder.append(" | Numero di partecipanti: ").append(participantsNumber);

        if (privacy != null) {
            stringBuilder.append(" | Privacy: ").append(privacy.equals("public") ? "pubblica" : privacy.equals("private") ? "privata" : "errore");
        }

        if (!getOptionals().isEmpty()) {
            stringBuilder.append(" | Altre richieste:");
            getOptionals().forEach(optional -> stringBuilder.append(" ").append(optional).append(","));
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        if (needs != null && !needs.isEmpty()) {
            stringBuilder.append(" | Esigenze particolari: ").append(needs);
        }

        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "BookingDto{" +
                "experience='" + experience + '\'' +
                ", participantsNumber=" + participantsNumber +
                ", datePc='" + datePc + '\'' +
                ", dateMobile='" + dateMobile + '\'' +
                ", time='" + time + '\'' +
                ", privacy='" + privacy + '\'' +
                ", guide=" + guide +
                ", equipment=" + equipment +
                ", transport=" + transport +
                ", insurance=" + insurance +
                ", lunch=" + lunch +
                ", breakfast=" + breakfast +
                ", needs='" + needs + '\'' +
                ", amount=" + amount +
                ", image='" + image + '\'' +
                '}';
    }
}


