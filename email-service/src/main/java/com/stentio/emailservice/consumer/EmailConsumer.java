package com.stentio.emailservice.consumer;

import com.stentio.emailservice.config.RabbitMQConfig;
import com.stentio.emailservice.dto.SendEmailRequest;
import com.stentio.emailservice.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public String consume(SendEmailRequest request) {
        try {
            emailService.send(request);
            return "EMAIL_SENT";
        } catch (MailException | IllegalArgumentException exception) {
            return "EMAIL_SEND_FAILED";
        }
    }
}