package com.incalabria.stripe_checkout.handler;

import com.incalabria.stripe_checkout.data.booking.BookingWebhookData;
import com.incalabria.stripe_checkout.data.booking.Others;
import com.incalabria.stripe_checkout.extractor.BookingWebhookDataExtractor;
import com.incalabria.stripe_checkout.service.SendGridEmailService;
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
    private SendGridEmailService sendGridEmailService;

    @Value("${email.to}")
    private String adminEmail;

    public void handleBookingPurchase(Session session) throws IOException {
        log.info("Handling booking purchase for session: {}", session.getId());

        BookingWebhookData bookingData = dataExtractor.extractBookingData(session);

        sendAdminConfirmationEmail(bookingData);
        sendCustomerConfirmationEmail(bookingData);

        log.info("Booking webhook processed successfully for session: {}", session.getId());
    }

    private void sendAdminConfirmationEmail(BookingWebhookData data) throws IOException {
        String adminEmailText = buildAdminEmailText(data);

        sendGridEmailService.sendEmail(adminEmail, "Pagamento autorizzato - Nuova prenotazione", adminEmailText);
        log.info("Admin confirmation email sent successfully");
    }

    private void sendCustomerConfirmationEmail(BookingWebhookData data) throws IOException {
        String customerEmailText = buildCustomerEmailText(data);

        try {
            sendGridEmailService.sendEmail(data.getCustomer().getEmail(),
                    "Richiesta presa in carico",
                    customerEmailText);
            log.info("Customer confirmation email sent to: {}", data.getCustomer().getEmail());
        } catch (IOException e) {
            log.error("Failed to send customer confirmation email to: {}", data.getCustomer().getEmail(), e);
            sendGridEmailService.sendEmail(adminEmail, "Errore di invio email al cliente",
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

        text.append("Total: ").append(String.format("%.2f", data.getTotal())).append("€");

        return text.toString();
    }

    private String buildCustomerEmailText(BookingWebhookData data) {
        String timeDisplay = convertTimeToItalian(data.getTime());
        String optionalsText = data.hasOtherRequests() ?
                String.format("• Altre richieste: %s\n", data.getOthers().stream()
                        .map(Others::getName)
                        .collect(Collectors.joining(", "))) :
                "";
        String needsText = data.hasNeeds() ?
                String.format("• Esigenze particolari: %s\n", data.getNeeds()) :
                "";

        return String.format("""
                Ciao %s,
                
                grazie per aver scelto InCalabria!
                Abbiamo ricevuto la tua richiesta di prenotazione e il nostro team la sta verificando.
                
                Dettagli della richiesta:
                • Esperienza: %s
                • Data: %s
                • Orario: %s
                • Numero di partecipanti: %s
                %s%s
                Cosa succede adesso:
                • La tua richiesta è in fase di approvazione.
                • Ti invieremo una mail di conferma appena l'esperienza sarà confermata e il pagamento verrà elaborato in modo sicuro.
                • Nessun addebito verrà effettuato finché non riceverai la nostra conferma.
                
                Nel frattempo, se desideri modificare la data o hai domande sull'esperienza, puoi contattarci rispondendo a questa mail o scrivendoci su Whatsapp al numero +39 3333286692.
                Non vediamo l'ora di farti vivere la Calabria più autentica tra natura e tradizioni.
                
                A presto,
                Il team di InCalabria
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
        return switch (time) {
            case "morning" -> "mattina";
            case "afternoon" -> "pomeriggio";
            default -> "?";
        };
    }
}
