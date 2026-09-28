package com.stentio.emailservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SmtpConfigRequest(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        String username,
        String password,
        @NotBlank @Pattern(regexp = "TLS|SSL|NONE") String encryption,
        String fromEmail,
        String fromName
) {
}