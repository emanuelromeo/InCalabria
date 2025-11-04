package com.incalabria.stripe_checkout.entity;

import com.incalabria.stripe_checkout.enumeration.GiftCardType;
import jakarta.persistence.*;

@Entity
public class GiftCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String code;
    @Enumerated(EnumType.STRING)
    private GiftCardType type;
    private String sender;
    private String receiver;
    private String message;
    private Double amount;

    public GiftCard(GiftCardType type, String sender, String receiver, String message) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.amount = (double) type.getAmount();
    }

    // getter e setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public GiftCardType getType() {
        return type;
    }

    public void setType(GiftCardType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

}

