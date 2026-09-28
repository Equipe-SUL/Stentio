package com.stentio.emailservice.controller;

import com.stentio.emailservice.dto.SmtpConfigRequest;
import com.stentio.emailservice.dto.SmtpConfigResponse;
import com.stentio.emailservice.dto.SmtpTestRequest;
import com.stentio.emailservice.dto.SmtpTestResponse;
import com.stentio.emailservice.service.SmtpConfigurationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/smtp")
public class SmtpController {

    private final SmtpConfigurationService service;

    public SmtpController(SmtpConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public SmtpConfigResponse getConfiguration() {
        return service.getConfiguration();
    }

    @PostMapping
    public SmtpConfigResponse saveConfiguration(@Valid @RequestBody SmtpConfigRequest request) {
        return service.saveConfiguration(request);
    }

    @PostMapping("/test")
    public SmtpTestResponse testConnection(@Valid @RequestBody SmtpTestRequest request) {
        return service.testConnection(request.config(), request.testEmail());
    }
}