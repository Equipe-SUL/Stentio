package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SendEmailRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(request.to());
        message.setSubject(request.subject());
        message.setText(request.body());

        JavaMailSender sender = mailSender != null
                ? mailSender
                : mailSenderFactory.create(configurationService.getStoredConfiguration());

        if (configurationService != null) {
            var configuration = configurationService.getConfiguration();
            if (configuration.fromEmail() != null && !configuration.fromEmail().isBlank()) {
                message.setFrom(configuration.fromEmail());
            }
        }

        sender.send(message);
    }
}