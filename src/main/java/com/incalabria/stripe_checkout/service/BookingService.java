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
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final String appDomain;
    private final EmailService emailService;
    private final GiftCardService giftCardService;
    private final BookingRepository bookingRepository;
    private final BookingWebhookDataExtractor bookingWebhookDataExtractor;

    @Autowired
    public BookingService(StripeProperties stripeProperties,
                          @Value("${app.domain}") String appDomain,
                          EmailService emailService,
                          GiftCardService giftCardService,
                          BookingRepository bookingRepository,
                          BookingWebhookDataExtractor bookingWebhookDataExtractor) {
        this.appDomain = appDomain;
        this.emailService = emailService;
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
                                                (booking.getCode() != null && !booking.getCode().isEmpty() ? String.format(" | Sconto applicato di %.2f€", discount) : ""))
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
                .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)
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

//    public void capturePaymentIntent(String sessionId) throws StripeException {
//        Session session = retrieveSession(sessionId);
//        log.info("Session with ID {} successfully retrieved", sessionId);
//
//        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
//        log.info("Payment Intent retrieved: {}", paymentIntent);
//
//        paymentIntent.capture(PaymentIntentCaptureParams.builder().build());
//        log.info("Payment Intent successfully captured");
//
//        BookingWebhookData bookingWebhookData = bookingWebhookDataExtractor.extractBookingData(session);
//        saveBooking(bookingWebhookData);
//        log.info("Booking successfully saved");
//    }

    public void capturePaymentIntent(
            String sessionId,
            String connectedAccountId,
            int providerPercentage
    ) throws RuntimeException {
        Session session = null;
        try {
            session = retrieveSession(sessionId);
        } catch (StripeException e) {
            log.error(e.getMessage());
            emailService.sendLog("Error in session retrieve", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("Session with ID {} successfully retrieved", sessionId);

        PaymentIntent paymentIntent = null;
        try {
            paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        } catch (StripeException e) {
            log.error(e.getMessage());
            emailService.sendLog("Error in payment intent retrieve", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("Payment Intent retrieved: {}", paymentIntent);

        // 3. Calcola la quota del fornitore e la quota InCalabria
        long totalAmount = paymentIntent.getAmount(); // centesimi
        long amountToProvider = Math.round(totalAmount * 0.96 * (providerPercentage / 100.0));
        long amountToAdmin = Math.round(totalAmount * 0.96 - amountToProvider);
        log.info("Total amount: " + totalAmount + " - amount to provider ("
                + providerPercentage + "%): " + amountToProvider + " - amount to admin: " + amountToAdmin);

        // 2. Cattura il PaymentIntent (autorizzazione -> addebito effettivo)
        try {
            paymentIntent = paymentIntent.capture(
                    PaymentIntentCaptureParams.builder().build()
            );

            // 2.1 Invia email di conferma al cliente
            sendBookingConfirmationEmail(session);
            emailService.sendLog("Email di conferma inviata", "L'email relativa alla sessione " + sessionId + " è stata correttamente inviata al cliente.");

            // 2.2 Salva la booking come confermata, ecc.
            BookingWebhookData bookingWebhookData = bookingWebhookDataExtractor.extractBookingData(session);
            saveBooking(bookingWebhookData, connectedAccountId, (double) amountToProvider / 100);
            log.info("Booking successfully saved");

        } catch (StripeException e) {
            log.error(e.getMessage());
            emailService.sendLog("Error in payment intent capture", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Payment Intent successfully captured: {}", paymentIntent.getId());
    }

    public Payout createPlatformPayout(long amountInCents) throws StripeException {

        PayoutCreateParams params = PayoutCreateParams.builder()
                .setAmount(amountInCents)  // es. 10000 = 100,00 €
                .setCurrency("eur")        // fisso in EUR
                .build();

        return Payout.create(params);
    }

    public Transfer transferToConnectedAccount(
            long amountInCents,
            String connectedAccountId
    ) throws StripeException {

        TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amountInCents)          // es. 5000 = 50,00 €
                .setCurrency("eur")                // fisso in EUR
                .setDestination(connectedAccountId) // "acct_..."
                .build();

        return Transfer.create(params);
    }

    public void cancelPaymentIntent(String sessionId) throws StripeException {
        Session session = retrieveSession(sessionId);
        log.info("Session with ID {} successfully retrieved", sessionId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
        log.info("Payment Intent retrieved: {}", paymentIntent);

        paymentIntent.cancel();
        log.info("Payment Intent successfully cancelled");

    }

    public void sendContactEmailToSupplier(Booking b) throws IOException {

        if (b.getCustomerNumber() == null || b.getCustomerEmail() == null) {
            throw new NullPointerException("Missing customer info");
        }

        String emailText = String.format("""
                    Ciao %s,
                    
                    ti inviamo i dati del cliente che parteciperà alla tua esperienza \"%s\" in data %s.
                    Nome cliente: %s
                    Telefono: %s
                    Email: %s
                    
                    Ti chiediamo, se possibile, di contattarlo il prima possibile per confermare orario, luogo di ritrovo e dettagli operativi.
                    
                    Grazie per la collaborazione e per valorizzare con noi la Calabria.
                    Il team di InCalabria
                    """,
                b.getSupplierName(),
                b.getExperience(),
                b.getExperienceDate().toString(),
                b.getCustomerName(),
                b.getCustomerNumber(),
                b.getCustomerEmail());
        try {
        emailService.sendEmail(b.getSupplerEmail(), "Contatti del cliente | Esperienza del " + b.getExperienceDate(), emailText);
        log.info("Contact email sent to supplier");
        } catch (IOException e) {
            throw new IOException("Error sending contact email to supplier.\nEmail content:\n\n" + emailText);
        }
    }

    public void sendContactEmailToCustomer(Booking b) throws IOException {

        if (b.getSupplierNumber() == null || b.getSupplerEmail() == null) {
            throw new NullPointerException("Missing supplier info");
        }

        String emailText = String.format("""
                    Ciao %s,
                    
                    manca poco alla tua esperienza \"%s\" e non vediamo l’ora che tu possa viverla!
                    Ecco i contatti del fornitore che ti accoglierà e sarà il tuo punto di riferimento:
                    Nome referente: %s
                    Telefono: %s
                    Email: %s
                    
                    Ti consigliamo di contattarlo il prima possibile per confermare orario, luogo di ritrovo ed eventuali dettagli utili.
                    
                    Se hai bisogno di supporto, siamo sempre qui per te.
                    Buona esperienza!
                    Il team di InCalabria
                    """,
                b.getCustomerName(),
                b.getExperience(),
                b.getSupplierName(),
                b.getSupplierNumber(),
                b.getSupplerEmail());

        String emailTextEng = String.format("""
                    Hi %s,
                    
                    your experience \"%s\" is just around the corner and we cannot wait for you to enjoy it!
                    Here are the supplier’s contact details of the person who will welcome you and be your point of reference:
                    Name: %s
                    Phone: %s
                    Email: %s
                    
                    We recommend contacting them as soon as possible to confirm the time, meeting point, and any useful details.
                    
                    If you need support, we're always here for you.
                    Enjoy your experience!
                    The InCalabria team
                    """,
                b.getCustomerName(),
                b.getExperience(),
                b.getSupplierName(),
                b.getSupplierNumber(),
                b.getSupplerEmail());
        try {
            emailService.sendEmail(b.getCustomerEmail(),
                    (b.getLanguage().name().equals("ITA") ? "Contatti del fornitore | Esperienza del " : "Supplier contact details | Experience on ") + b.getExperienceDate(),
                    b.getLanguage().name().equals("ITA") ? emailText : emailTextEng);
            log.info("Contact email sent to customer");
        } catch (IOException e) {
            throw new IOException("Error sending contact email to customer.\nEmail content:\n\n" + (b.getLanguage().name().equals("ITA") ? emailText : emailTextEng));
        }
    }

    public void sendReviewEmail(Booking b) throws IOException {
        String emailText = String.format("""
                    Ciao %s,
                    
                    speriamo che la tua esperienza con InCalabria sia stata speciale e che ti abbia fatto scoprire un nuovo pezzo di questa terra meravigliosa.
                    
                    Ci farebbe piacere sapere com’è andata!
                    La tua opinione è preziosa sia per noi che per i viaggiatori che verranno dopo di te.
                    
                    Lascia la tua recensione qui: https://tally.so/r/n9LYpE
                    
                    Grazie per aver scelto di vivere la Calabria con noi.
                    A presto!
                    Il team di InCalabria
                    """,
                b.getCustomerName());

        String emailTextEng = String.format("""
                    Hi %s,
                    
                    we hope your experience with InCalabria was special and helped you discover a new piece of this wonderful land.
                    
                    We would love to know how it went!
                    Your opinion is valuable both for us and for the travellers who will come after you.
                    
                    Leave your review here: https://tally.so/r/wAoYje
                    
                    Thank you for choosing to experience Calabria with us.
                    See you soon!
                    The InCalabria team
                    """,
                b.getCustomerName());

        emailService.sendEmail(b.getCustomerEmail(),
                b.getLanguage().name().equals("ITA") ? "Com'è andata la tua esperienza InCalabria?" : "How was your InCalabria experience?",
                b.getLanguage().name().equals("ITA") ? emailText : emailTextEng);
        log.info("Review email sent to customer");
    }

    private void sendBookingConfirmationEmail(Session session) throws IOException {
        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            double amount = (double) session.getAmountTotal() / 100;
            String experience = session.getMetadata().get("experience");
            String language = session.getMetadata().get("language");

            String emailText = String.format("""
                    Ciao %s,
                    
                    siamo felici di confermare la tua prenotazione con InCalabria!
                    Il pagamento di %.2f€ è andato a buon fine e la tua esperienza \"%s\" è ufficialmente prenotata.
                    
                    Riceverai un'altra email con maggiorni informazioni su luogo e orario di incontro e i contatti del fornitore tre giorni prima dell'esperienza.
                    Nel frattempo, se hai domande o desideri personalizzare la tua esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su whatsapp al numero +39 3333286692.
                    Preparati a vivere la Calabria più autentica, tra mare, natura e tradizioni locali.
                    
                    A presto,
                    Il team di InCalabria
                    """, customerName, amount, experience);

            String emailTextEng = String.format("""
                    Hi %s,
                    
                    we are happy to confirm your booking with InCalabria!
                    The payment of %.2f€ was successful and your experience \"%s\" is officially booked.
                    
                    You will receive another email with more information on the meeting place and time and the supplier’s contacts three days before the experience.
                    In the meantime, if you have any questions or wish to customize your experience, you can contact us by replying to this email or writing to us on WhatsApp at +39 3333286692.
                    Get ready to experience the most authentic Calabria, between sea, nature and local traditions.
                    
                    See you soon,
                    The InCalabria Team
                    """, customerName, amount, experience);

            emailService.sendEmail(customerEmail,
                    language.equals("ITA") ? "Prenotazione confermata!" : "Booking confirmed!",
                    language.equals("ITA") ? emailText : emailTextEng);
            log.info("Confirmation email sent to the customer");
        }
    }

    public void sendBookingCancellationEmail(Session session) throws IOException {
        if (session.getCustomerDetails() != null) {
            String customerEmail = session.getCustomerDetails().getEmail();
            String customerName = session.getCustomerDetails().getName();
            String experience = session.getMetadata().get("experience");
            String language = session.getMetadata().get("language");

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

            String emailTextEng = String.format("""
                    Hi %s,
                    
                    thank you for choosing InCalabria and for your interest in our experiences.
                    Unfortunately, at this time we are unable to confirm your request for the experience \"%s\" due to lack of availability.
                    We are truly sorry for the inconvenience, but we hope to welcome you soon in one of our other activities.
                   
                    We invite you to check our website [www.incalabria.net](https://www.incalabria.net) to discover other experiences available on the same dates or in alternative periods.
                    For any doubts or requests, you can write to us on WhatsApp at +39 3333286692; we will be happy to help you find an alternative.
                   
                    Thank you again for your trust,
                    The InCalabria Team
                    """, customerName, experience);

            emailService.sendEmail(customerEmail,
                    language.equals("ITA") ? "Richiesta rifiutata" : "Request denied",
                    language.equals("ITA") ? emailText : emailTextEng);
            log.info("Cancellation email sent to the customer");
        }
    }

    public void saveBooking(BookingWebhookData data, String connectedAccountId, double supplierAmount) throws StripeException {
        Booking b = new Booking();
        b.setSessionId(data.getSessionId());
        b.setExperience(data.getExperience());
        b.setTotalAmount(data.getTotal());
        b.setSupplierAmount(supplierAmount);
        b.setCustomerEmail(data.getCustomer().getEmail());
        b.setCustomerName(data.getCustomer().getName());
        b.setCustomerNumber(data.getCustomer().getPhone());
        b.setExperienceDate(LocalDate.parse(data.getDate()));
        b.setLanguage(data.getLanguage());
        b.setReviewEmailSent(false);
        b.setContactEmailSent(false);
        b.setTransferSent(false);
        b.setPayoutSent(false);
        Account connectedAccount = Account.retrieve(connectedAccountId);
        b.setSupplerEmail(connectedAccount.getEmail());
        b.setSupplierName(connectedAccount.getBusinessProfile().getName());
        b.setSupplierNumber(connectedAccount.getBusinessProfile().getSupportPhone());
        b.setSupplierId(connectedAccountId);
        bookingRepository.save(b);
    }

}
