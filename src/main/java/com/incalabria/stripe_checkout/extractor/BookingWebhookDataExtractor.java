package com.incalabria.stripe_checkout.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.dto.BookingWebhookData;
import com.incalabria.stripe_checkout.dto.OtherRequest;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class BookingWebhookDataExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BookingWebhookData extractBookingData(Session session) {
        Map<String, String> metadata = session.getMetadata();

        List<OtherRequest> otherRequests = Collections.emptyList();
        String othersJson = metadata.get("others");
        if (othersJson != null && !othersJson.isEmpty()) {
            try {
                otherRequests = objectMapper.readValue(
                        othersJson,
                        new TypeReference<List<OtherRequest>>() {}
                );
            } catch (Exception e) {
                // Log e procedi con lista vuota
                e.printStackTrace();
            }
        }

        return new BookingWebhookData(
                session.getId(),
                session.getCustomerDetails().getName(),
                session.getCustomerDetails().getEmail(),
                session.getCustomerDetails().getPhone(),
                metadata.get("experience"),
                metadata.get("participants"),
                metadata.get("date"),
                metadata.get("time"),
                metadata.get("pickup"),
                otherRequests,
                metadata.get("needs"),
                session.getAmountTotal() / 100.0
        );
    }
}
