package com.incalabria.stripe_checkout.service;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    private final SendGrid sendGrid;
    private final Email fromEmail;

    public SendGridEmailService(@Value("${sendgrid.api.key}") SendGrid sendGrid,
                                @Value("${email.from}") String from,
                                @Value("${email.name}") String name) {
        this.sendGrid = sendGrid;
        this.fromEmail = new Email(from, name);
    }


    public void sendEmail(String to, String subject, String body) throws IOException {

        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(fromEmail, subject, toEmail, content);

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

