package com.incalabria.stripe_checkout.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.data.booking.Customer;
import com.incalabria.stripe_checkout.data.booking.Others;
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

        List<Others> others = Collections.emptyList();
        String othersJson = metadata.get("others");
        if (othersJson != null && !othersJson.isEmpty()) {
            try {
                others = objectMapper.readValue(
                        othersJson,
                        new TypeReference<List<Others>>() {}
                );
            } catch (Exception e) {
                // Log e procedi con lista vuota
                e.printStackTrace();
            }
        }

        return new BookingWebhookData(
                session.getId(),
                new Customer(
                        session.getCustomerDetails().getName(),
                        session.getCustomerDetails().getEmail(),
                        session.getCustomerDetails().getPhone(),
                        session.getCustomFields().stream()
                                .filter(field -> "taxId".equals(field.getKey()))
                                .map(field -> field.getText() != null ? field.getText().getValue() : null)
                                .filter(java.util.Objects::nonNull)
                                .findFirst()
                                .orElse(null),
                        session.getCustomerDetails().getAddress()
                ),
                metadata.get("experience"),
                metadata.get("participants"),
                metadata.get("date"),
                metadata.get("time"),
                metadata.get("pickup"),
                others,
                metadata.get("needs"),
                session.getAmountTotal() / 100.0
        );
    }
}
