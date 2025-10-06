package com.incalabria.stripe_checkout.service;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    private final SendGrid sendGrid;

    public SendGridEmailService() {
        this.sendGrid = new SendGrid(System.getenv("SENDGRID_API_KEY"));
    }

    public void sendEmail(String to, String subject, String body) throws IOException {
        Email from = new Email("info@bearound.eu");
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        if (response.getStatusCode() >= 400) {
            throw new IOException("Errore invio email: " + response.getBody());
        }
    }
}

