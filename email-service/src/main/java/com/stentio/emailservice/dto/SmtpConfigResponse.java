package com.stentio.emailservice.dto;

public record SmtpConfigResponse(
        String host,
        int port,
        String username,
        String password,
        String encryption,
        String fromEmail,
        String fromName
) {
}