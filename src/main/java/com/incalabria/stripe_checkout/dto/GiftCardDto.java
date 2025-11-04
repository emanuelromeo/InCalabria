package com.incalabria.stripe_checkout.dto;

import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.enumeration.GiftCardType;

public class GiftCardDto {
    private GiftCardType type;
    private String sender;
    private String receiver;
    private String message;

    public GiftCardDto() {
    }

    // getter e setter

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

    public GiftCard toGiftCard() {
        return new GiftCard(type, sender, receiver, message);
    }

}
