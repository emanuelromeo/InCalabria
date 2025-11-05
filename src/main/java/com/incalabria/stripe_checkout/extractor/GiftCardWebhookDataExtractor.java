package com.incalabria.stripe_checkout.extractor;

import com.incalabria.stripe_checkout.data.giftcard.GiftCardWebhookData;
import com.incalabria.stripe_checkout.enumeration.GiftCardType;
import com.stripe.model.checkout.Session;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GiftCardWebhookDataExtractor {
    public GiftCardWebhookData extractGiftCardData(Session session) {
        Map<String, String> metadata = session.getMetadata();
        GiftCardWebhookData dto = new GiftCardWebhookData();
        String typeStr = metadata.get("type");
        if (typeStr != null) {
            dto.setType(GiftCardType.valueOf(typeStr));
        }
        dto.setSender(metadata.get("sender"));
        dto.setReceiver(metadata.get("receiver"));
        dto.setMessage(metadata.get("message"));
        return dto;
    }
}
