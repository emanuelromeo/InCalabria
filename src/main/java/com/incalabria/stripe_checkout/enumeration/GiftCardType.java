package com.incalabria.stripe_checkout.enumeration;

public enum GiftCardType {
    GOLD(
            150,
            "#8C7236", "#B99645", "#F6CD61", "#BD9E40", "#C19F47", "#907A23",
            "#5d4b23",
            "https://i.postimg.cc/tTVJ85gh/In-Calabria-gold.png",
            true,
            "#d0b450"
    ),
    SILVER(
            80,
            "#878787", "#BDBDBD", "#F4F4F4", "#C1C1C1", "#C9C9C9", "#979797",
            "#7A7A7A",  // Schiarito da #5D5D5D (più grigio chiaro)
            "https://i.postimg.cc/mk39x3SD/In-Calabria-silver.png",
            true,
            "#d0d0d0"
    ),
    BLACK(
            50,
            "#090909", "#0E0E0E", "#252525", "#000000", "#1B1B1B", "#090909",
            "#FFFFFF",
            "https://i.postimg.cc/C1cycX3r/In-Calabria-B-G.png",
            false,
            null
    ),
    GREEN(
            30,
            "#024035", "#04473B", "#065C4D", "#034337", "#025547", "#024035",
            "#FFFFFF",
            "https://i.postimg.cc/C1cycX3r/In-Calabria-B-G.png",
            false,
            null
    );

    private final int amount;
    private final String gradientColor1;
    private final String gradientColor2;
    private final String gradientColor3;
    private final String gradientColor4;
    private final String gradientColor5;
    private final String gradientColor6;
    private final String textBackgroundColor;
    private final String logoUrl;
    private final boolean hasInnerShadow;
    private final String shadowColor;

    GiftCardType(
            int amount,
            String gradientColor1, String gradientColor2, String gradientColor3,
            String gradientColor4, String gradientColor5, String gradientColor6,
            String textBackgroundColor,
            String logoUrl,
            boolean hasInnerShadow,
            String shadowColor) {
        this.amount = amount;
        this.gradientColor1 = gradientColor1;
        this.gradientColor2 = gradientColor2;
        this.gradientColor3 = gradientColor3;
        this.gradientColor4 = gradientColor4;
        this.gradientColor5 = gradientColor5;
        this.gradientColor6 = gradientColor6;
        this.textBackgroundColor = textBackgroundColor;
        this.logoUrl = logoUrl;
        this.hasInnerShadow = hasInnerShadow;
        this.shadowColor = shadowColor;
    }

    public int getAmount() { return amount; }
    public String getGradientColor1() { return gradientColor1; }
    public String getGradientColor2() { return gradientColor2; }
    public String getGradientColor3() { return gradientColor3; }
    public String getGradientColor4() { return gradientColor4; }
    public String getGradientColor5() { return gradientColor5; }
    public String getGradientColor6() { return gradientColor6; }
    public String getTextBackgroundColor() { return textBackgroundColor; }
    public String getLogoUrl() { return logoUrl; }
    public boolean hasInnerShadow() { return hasInnerShadow; }
    public String getShadowColor() { return shadowColor; }
}
