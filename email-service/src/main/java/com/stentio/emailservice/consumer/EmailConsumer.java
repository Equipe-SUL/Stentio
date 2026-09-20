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
    public void consume(SendEmailRequest request) {
        emailService.send(request);
    }
}