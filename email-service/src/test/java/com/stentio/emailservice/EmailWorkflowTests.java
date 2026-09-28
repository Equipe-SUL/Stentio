package com.stentio.emailservice;

import com.stentio.emailservice.consumer.EmailConsumer;
import com.stentio.emailservice.dto.SendEmailRequest;
import com.stentio.emailservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailWorkflowTests {

    @Test
    void shouldSendEmailWithRequiredFields() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailService service = new EmailService(mailSender);

        SendEmailRequest request = new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto do email",
                "Mensagem de teste"
        );

        service.send(request);

        assertEquals(1, mailSender.sentMessages.size());
        SimpleMailMessage message = mailSender.sentMessages.get(0);
        assertEquals("destinatario@teste.com", message.getTo()[0]);
        assertEquals("Assunto do email", message.getSubject());
        assertEquals("Mensagem de teste", message.getText());
    }

    @Test
    void shouldRejectRequestWhenRequiredFieldsAreMissing() {
        EmailService service = new EmailService(new RecordingMailSender());

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("", "Assunto", "Mensagem"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("destinatario@teste.com", "", "Mensagem"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("destinatario@teste.com", "Assunto", ""))
        );
    }

    @Test
    void consumerShouldReturnFailureMessageWhenEmailCannotBeSent() {
        EmailConsumer consumer = new EmailConsumer(new EmailService(new FailingMailSender()));

        String result = consumer.consume(new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto",
                "Mensagem"
        ));

        assertEquals("EMAIL_SEND_FAILED", result);
    }

    @Test
    void consumerShouldReturnSuccessMessageWhenEmailIsSent() {
        EmailConsumer consumer = new EmailConsumer(new EmailService(new RecordingMailSender()));

        String result = consumer.consume(new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto",
                "Mensagem"
        ));

        assertEquals("EMAIL_SENT", result);
    }

    private static class RecordingMailSender implements JavaMailSender {
        private final List<SimpleMailMessage> sentMessages = new ArrayList<>();

        @Override
        public MimeMessage createMimeMessage() {
            return null;
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            return null;
        }

        @Override
        public void send(MimeMessage mimeMessage) {
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            sentMessages.add(simpleMessage);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage message : simpleMessages) {
                sentMessages.add(message);
            }
        }
    }

    private static class FailingMailSender implements JavaMailSender {
        @Override
        public MimeMessage createMimeMessage() {
            return null;
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            return null;
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            throw new MailException("Simulated send failure") {
            };
        }
    }
}
