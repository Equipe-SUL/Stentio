package com.stentio.emailservice.controller;

import com.stentio.emailservice.dto.SendEmailRequest;
import com.stentio.emailservice.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public String sendEmail(
            @Valid @RequestBody SendEmailRequest request
    ) {
        emailService.send(request);

        return "E-mail enviado!";
    }
}