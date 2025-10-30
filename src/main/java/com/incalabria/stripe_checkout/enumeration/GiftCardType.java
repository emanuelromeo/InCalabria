package com.incalabria.stripe_checkout.enumeration;

public enum GiftCardType {
    GREEN("green"),
    BLACK("black"),
    SILVER("silver"),
    GOLD("gold");

    private final String fileName;

    GiftCardType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return "/giftcards/" + fileName.toUpperCase() + ".pdf";
    }
}

