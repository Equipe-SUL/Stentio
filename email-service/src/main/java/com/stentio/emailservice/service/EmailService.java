package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SendEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SmtpConfigurationService configurationService;
    private final SmtpMailSenderFactory mailSenderFactory;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.configurationService = null;
        this.mailSenderFactory = null;
    }

    @Autowired
    public EmailService(
            SmtpConfigurationService configurationService,
            SmtpMailSenderFactory mailSenderFactory
    ) {
        this.mailSender = null;
        this.configurationService = configurationService;
        this.mailSenderFactory = mailSenderFactory;
    }

    public void send(SendEmailRequest request) {
        if (request == null || request.to() == null || request.to().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.body() == null || request.body().isBlank()) {
            throw new IllegalArgumentException("Os campos to, subject e body são obrigatórios");
        }

        JavaMailSender sender = mailSender != null
                ? mailSender
                : mailSenderFactory.create(configurationService.getStoredConfiguration());

        MimeMessage mimeMessage = sender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            message.setTo(request.to());
            message.setSubject(request.subject());
            message.setText(request.body());

            if (configurationService != null) {
                var configuration = configurationService.getConfiguration();
                if (configuration.fromEmail() != null && !configuration.fromEmail().isBlank()) {
                    message.setFrom(buildFromAddress(configuration.fromEmail(), configuration.fromName()));
                }
            }
        } catch (MessagingException exception) {
            throw new MailPreparationException("Falha ao montar a mensagem de e-mail", exception);
        }

        sender.send(mimeMessage);
    }

    private InternetAddress buildFromAddress(String email, String name) {
        try {
            if (name != null && !name.isBlank()) {
                return new InternetAddress(email, name, "UTF-8");
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        try {
            return new InternetAddress(email);
        } catch (AddressException exception) {
            throw new IllegalArgumentException("E-mail do remetente inválido: " + email, exception);
        }
    }
}