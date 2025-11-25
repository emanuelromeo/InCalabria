package com.incalabria.stripe_checkout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incalabria.stripe_checkout.config.StripeProperties;
import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.entity.Booking;
import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.extractor.BookingWebhookDataExtractor;
import com.incalabria.stripe_checkout.repository.BookingRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final String appDomain;
    private final SendGridEmailService sendGridEmailService;
    private final GiftCardService giftCardService;
    private final BookingRepository bookingRepository;
    private final BookingWebhookDataExtractor bookingWebhookDataExtractor;

    @Autowired
    public BookingService(StripeProperties stripeProperties,
                          @Value("${app.domain}") String appDomain,
                          SendGridEmailService sendGridEmailService,
                          GiftCardService giftCardService,
                          BookingRepository bookingRepository,
                          BookingWebhookDataExtractor bookingWebhookDataExtractor) {
        this.appDomain = appDomain;
        this.sendGridEmailService = sendGridEmailService;
        this.giftCardService = giftCardService;
        this.bookingRepository = bookingRepository;
        this.bookingWebhookDataExtractor = bookingWebhookDataExtractor;
        com.stripe.Stripe.apiKey = stripeProperties.getApi().getSecretKey();
    }

    public Session createCheckoutSession(BookingWebhookData booking) throws StripeException {

        log.info("Booking info:\n{}", booking);

        double discount = 0;

        Optional<GiftCard> giftCard = giftCardService.getGiftCard(booking.getCode());
        if (!giftCard.isEmpty()) {
            discount = Math.min(giftCard.get().getAmount(), booking.getTotal());
        }

        long baseAmountInCents = (long) ((booking.getTotal() - discount) * 100);
        long commissionAmount = (long) (baseAmountInCents * 0.04);


        // Riga esperienza principale
        SessionCreateParams.LineItem baseItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setUnitAmount(baseAmountInCents)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(booking.getExperience())
                                .setDescription(booking.getBookingDescription() +
                                                (booking.getCode() != null ? String.format(" | Sconto applicato di %.2f€", discount) : ""))
                                .build())
                        .build())
                .setQuantity(1L)
                .build();

        // Riga commissione aggiuntiva 4%
        SessionCreateParams.LineItem commissionItem = SessionCreateParams.LineItem.builder()
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("eur")
                        .setUnitAmount(commissionAmount)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("Commissioni e tasse")
                                .build())
                        .build())
                .setQuantity(1L)
                .build();

        SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                .build();

        String successUrl = appDomain + "/experiences/success";
        String cancelUrl = appDomain + "/";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productType", "booking");
        metadata.put("experience", booking.getExperience());
        metadata.put("participants", String.valueOf(booking.getParticipants()));
        metadata.put("date", booking.getDate());
        metadata.put("time", booking.getTime());
        metadata.put("needs", booking.getNeeds());
        metadata.put("pickup", booking.getPickup());
        metadata.put("code", booking.getCode());
        metadata.put("discount", String.valueOf(discount));
        metadata.put("language", booking.getLanguage().name());

        if (booking.hasOtherRequests()) {
            ObjectMapper objectMapper = new ObjectMapper();
            String othersJson = null;
            try {
                othersJson = objectMapper.writeValueAsString(booking.getOthers());
            } catch (JsonProcessingException e) {
                log.error("Can't parse others: " + e.getMessage());
            }
            metadata.put("others", othersJson);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(baseItem)
                .addLineItem(commissionItem)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addCustomField(
                        SessionCreateParams.CustomField.builder()
                                .setKey("taxId")
                                .setLabel(
                                        SessionCreateParams.CustomField.Label.builder()
                                                .setType(SessionCreateParams.CustomField.Label.Type.CUSTOM)
                                                .setCustom("Codice Fiscale (o P.IVA)")
                                                .build()
                                )
                                .setType(SessionCreateParams.CustomField.Type.TEXT)
                                .build()
                )
                .setPhoneNumberCollection(SessionCreateParams.PhoneNumberCollection.builder().setEnabled(true).build())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setPaymentIntentData(paymentIntentData)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.KLARNA)
                .build();

        log.info("Building checkout session with additional 4% commission...");
        return Session.create(params);
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    public void capturePaymentIntent(String sessionId) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID " + sessionId + " successfully retrieved");

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: " + paymentIntent);

        paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
        log.info("Payment Intent successfully captured");

        BookingWebhookData bookingWebhookData = bookingWebhookDataExtractor.extractBookingData(session);
        saveBooking(bookingWebhookData);
        log.info("Booking successfully saved");
    }

    public void capturePaymentIntentAndTransferToProvider(
            String sessionId,
            String connectedAccountId,
            int providerPercentage // 78 o 80
    ) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID " + sessionId + " successfully retrieved");

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: " + paymentIntent);

        // 1. Cattura il PaymentIntent
        paymentIntent = paymentIntent.capture(
                PaymentIntentCaptureParams.builder().build()
        );
        log.info("Payment Intent successfully captured: " + paymentIntent.getId());

        // 2. Recupera la charge associata (latest_charge)
        String chargeId = paymentIntent.getLatestCharge();
        if (chargeId == null) {
            throw new IllegalStateException("No charge found on PaymentIntent " + paymentIntent.getId());
        }
        log.info("Latest charge ID: " + chargeId);

        // 3. Calcola importo da mandare al fornitore
        long totalAmount = paymentIntent.getAmount(); // in centesimi
        long amountToProvider = Math.round(totalAmount * (providerPercentage / 100.0));
        log.info("Total amount: " + totalAmount + " - amount to provider ("
                + providerPercentage + "%): " + amountToProvider);

        // 4. Crea il Transfer verso il fornitore
        TransferCreateParams transferParams = TransferCreateParams.builder()
                .setAmount(amountToProvider)
                .setCurrency(paymentIntent.getCurrency())
                .setDestination(connectedAccountId)
                .setSourceTransaction(chargeId) // collega il transfer a questa charge
                .build();

        Transfer transfer = Transfer.create(transferParams);
        log.info("Transfer created to provider " + connectedAccountId + ": " + transfer.getId());
    }


    public void cancelPaymentIntent(String sessionId) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID " + sessionId + " successfully retrieved");

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: " + paymentIntent);

        paymentIntent.cancel();
        log.info("Payment Intent successfully cancelled");

    }

    public void sendBookingConfirmationEmail(Session session) throws IOException {
        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            double amount = (double) session.getAmountTotal() / 100;
            String experience = session.getMetadata().get("experience");
            String emailText = String.format("""
                    Ciao %s,
                    
                    siamo felici di confermare la tua prenotazione con InCalabria!
                    Il pagamento di %.2f€ è andato a buon fine e la tua esperienza \"%s\" è ufficialmente prenotata.
                    
                    Nel frattempo, se hai domande o desideri personalizzare la tua esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su whatsapp al numero +39 3333286692.
                    Preparati a vivere la Calabria più autentica, tra mare, natura e tradizioni locali.
                    
                    A presto,
                    Il team di InCalabria
                    """, customerName, amount, experience);
            sendGridEmailService.sendEmail(customerEmail, "Prenotazione confermata!", emailText);
            log.info("Confirmation email sent to the customer");
        }
    }

    public void sendBookingCancellationEmail(Session session) throws IOException {
        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            String experience = session.getMetadata().get("experience");
            String emailText = String.format("""
                    Ciao %s,
                    
                    ti ringraziamo per aver scelto InCalabria e per l’interesse verso le nostre esperienze.
                    Purtroppo, in questo momento non siamo in grado di confermare la tua richiesta per l’esperienza \"%s\", a causa della mancanza di disponibilità.
                    Siamo davvero spiacenti per l’inconveniente, ma ci auguriamo di poterti accogliere presto in un’altra delle nostre attività.
                    
                    Ti invitiamo a consultare il nostro sito [www.incalabria.net](https://www.incalabria.net) per scoprire altre esperienze disponibili nelle stesse date o in periodi alternativi.
                    Per qualsiasi dubbio o richiesta, puoi scriverci su Whatsapp al numero +39 3333286692, saremo felici di aiutarti a trovare un’alternativa.
                    
                    Grazie ancora per la fiducia,
                    Il team di InCalabria
                    """, customerName, experience);
            sendGridEmailService.sendEmail(customerEmail, "Richiesta rifiutata", emailText);
            log.info("Cancellation email sent to the customer");
        }
    }

    public void saveBooking(BookingWebhookData data) {
        Booking b = new Booking();
        b.setSessionId(data.getSessionId());
        b.setCustomerEmail(data.getCustomer().getEmail());
        b.setCustomerName(data.getCustomer().getName());
        b.setExperienceDate(LocalDate.parse(data.getDate()));
        b.setLanguage(data.getLanguage());
        b.setReviewEmailSent(false);
        bookingRepository.save(b);
    }

}
