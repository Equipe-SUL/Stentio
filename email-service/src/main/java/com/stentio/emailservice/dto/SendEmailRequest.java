package com.stentio.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(

        @NotBlank
        @Email
        String to,

        @NotBlank
        String subject,

        @NotBlank
        String body

) {}