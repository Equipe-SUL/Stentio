package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SendEmailRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(SendEmailRequest request) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(request.to());
        message.setSubject(request.subject());
        message.setText(request.body());

        mailSender.send(message);
    }
}