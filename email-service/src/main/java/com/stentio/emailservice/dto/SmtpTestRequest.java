package com.stentio.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SmtpTestRequest(
                @NotBlank String host,
                @Min(1) @Max(65535) int port,
                String username,
                String password,
                @NotBlank String encryption,
                String fromEmail,
                String fromName,
        @NotBlank @Email String testEmail
) {

        public SmtpConfigRequest config() {
                return new SmtpConfigRequest(host, port, username, password, encryption, fromEmail, fromName);
        }
}