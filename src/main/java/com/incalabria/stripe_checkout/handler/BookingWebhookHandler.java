package com.incalabria.stripe_checkout.handler;

import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.data.booking.Others;
import com.incalabria.stripe_checkout.extractor.BookingWebhookDataExtractor;
import com.incalabria.stripe_checkout.service.GiftCardService;
import com.incalabria.stripe_checkout.service.EmailService;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class BookingWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingWebhookHandler.class);

    @Autowired
    private BookingWebhookDataExtractor dataExtractor;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GiftCardService giftCardService;

    public void handleBookingPurchase(Session session) throws IOException {
        log.info("Handling booking purchase for session: {}", session.getId());

        BookingWebhookData bookingData = dataExtractor.extractBookingData(session);
        if (bookingData.getDiscount() > 0) {
            giftCardService.withdrawFromGiftCard(bookingData.getCode(), bookingData.getDiscount());
        }

        sendAdminConfirmationEmail(bookingData);
        sendCustomerConfirmationEmail(bookingData);

        log.info("Booking webhook processed successfully for session: {}", session.getId());
    }

    private void sendAdminConfirmationEmail(BookingWebhookData data) throws IOException {
        String adminEmailText = buildAdminEmailText(data);

        emailService.sendLog( "Pagamento autorizzato - Nuova prenotazione", adminEmailText);
        log.info("Admin confirmation email sent successfully");
    }

    private void sendCustomerConfirmationEmail(BookingWebhookData data) throws IOException {
        String customerEmailText = buildCustomerEmailText(data);

        try {
            emailService.sendEmail(data.getCustomer().getEmail(),
                    data.getLanguage().name().equals("ITA") ? "Richiesta presa in carico" : "Request received",
                    customerEmailText);
            log.info("Customer confirmation email sent to: {}", data.getCustomer().getEmail());
        } catch (IOException e) {
            log.error("Failed to send customer confirmation email to: {}", data.getCustomer().getEmail(), e);
            emailService.sendLog( "Errore di invio email al cliente",
                    String.format("Session ID: %s\nErrore: %s", data.getSessionId(), e.getMessage()));
            throw e;
        }
    }

    private String buildAdminEmailText(BookingWebhookData data) {
        StringBuilder text = new StringBuilder();
        text.append("Session ID: ").append(data.getSessionId()).append("\n");
        text.append("Customer: ").append(data.getCustomer()).append("\n");
        text.append("Experience: ").append(data.getExperience()).append("\n");
        text.append("Participants: ").append(data.getParticipants()).append("\n");
        text.append("Date: ").append(data.getDate()).append("\n");
        text.append("Time: ").append(data.getTime()).append("\n");
        text.append("Pickup: ").append(data.getPickup()).append("\n");

        if (data.hasOtherRequests()) {
            text.append("Others:\n");
            for (Others req : data.getOthers()) {
                text.append("• ").append(req.getName())
                        .append(" (").append(String.format("%.2f€", req.getCost())).append(")\n");
            }
        }


        if (data.hasNeeds()) {
            text.append("Needs: ").append(data.getNeeds()).append("\n");
        }

        text.append("Language: ").append(data.getLanguage().name()).append("\n");

        if (data.getCode() != null) {
            text.append("Code: ").append(data.getCode()).append("\n");
            text.append("Discount: ").append(String.format("%.2f€", data.getDiscount())).append("\n");
        }

        text.append("Total: ").append(String.format("%.2f€", data.getTotal()));

        return text.toString();
    }

    private String buildCustomerEmailText(BookingWebhookData data) {
        boolean isItalian = data.getLanguage() == null || data.getLanguage().name().equals("ITA");
        String timeDisplay = isItalian ? convertTimeToItalian(data.getTime()) : convertTimeToEnglish(data.getTime());
        String optionalsText = data.hasOtherRequests() ?
                String.format(isItalian ? "• Altre richieste: %s\n" : "• Other requests: %s\n", data.getOthers().stream()
                        .map(Others::getName)
                        .collect(Collectors.joining(", "))) :
                "";
        String needsText = data.hasNeeds() ?
                String.format(isItalian ? "• Esigenze particolari: %s\n" : "• Special requirements: %s\n", data.getNeeds()) :
                "";

        return String.format(isItalian ? """
                Ciao %s,
                
                grazie per aver scelto InCalabria!
                Abbiamo ricevuto la tua richiesta di prenotazione e il nostro team la sta verificando.
                
                Dettagli della richiesta:
                • Esperienza: %s
                • Data: %s
                %s• Numero di partecipanti: %s
                %s%s
                Cosa succede adesso:
                • La tua richiesta è in fase di approvazione.
                • Ti invieremo una mail di conferma appena l'esperienza sarà confermata e il pagamento verrà elaborato in modo sicuro.
                • Nessun addebito verrà effettuato finché non riceverai la nostra conferma.
                
                Nel frattempo, se desideri modificare la data o hai domande sull'esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su Whatsapp al numero +39 3333286692.
                Non vediamo l'ora di farti vivere la Calabria più autentica tra natura e tradizioni.
                
                A presto,
                Il team di InCalabria
                """ : """
                Hi %s,
                
                thank you for choosing InCalabria!
                We have received your booking request and our team is currently reviewing it.
                
                Request details:
                • Experience: %s
                • Date: %s
                %s• Number of participants: %s
                %s%s
                What happens next:
                • Your request is pending approval.
                • We will send you a confirmation email as soon as the experience is confirmed and the payment is processed securely.
                • No charges will be made until you receive our confirmation.
                
                In the meantime, if you would like to change the date or have any questions about the experience, you can contact us by replying to this email or by texting us on WhatsApp at +39 3333286692.
                We look forward to helping you experience the most authentic Calabria, among nature and traditions.
                
                See you soon,
                The InCalabria team
                """,
                data.getCustomer().getName(),
                data.getExperience(),
                data.getDate(),
                timeDisplay,
                data.getParticipants(),
                optionalsText,
                needsText);
    }

    private String convertTimeToItalian(String time) {
        if (time == null) {
            return "";
        }
        return switch (time) {
            case "morning" -> "• Orario: mattina\n";
            case "afternoon" -> "• Orario: pomeriggio\n";
            default -> "";
        };
    }

    private String convertTimeToEnglish(String time) {
        if (time == null) {
            return "";
        }
        return switch (time) {
            case "morning" -> "• Time: morning\n";
            case "afternoon" -> "• Time: afternoon\n";
            default -> "";
        };
    }
}
