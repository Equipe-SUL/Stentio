package com.stentio.emailservice.controller;

import com.stentio.emailservice.dto.SmtpConfigRequest;
import com.stentio.emailservice.dto.SmtpConfigResponse;
import com.stentio.emailservice.dto.SmtpTestResponse;
import com.stentio.emailservice.service.SmtpConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SmtpControllerTest {

    private final SmtpConfigurationService service = mock(SmtpConfigurationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SmtpController(service)).build();
    }

    @Test
    void shouldReturnSavedSmtpConfiguration() throws Exception {
        when(service.getConfiguration()).thenReturn(configuration());

        mockMvc.perform(get("/api/v1/smtp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.example.com"))
                .andExpect(jsonPath("$.port").value(587))
                .andExpect(jsonPath("$.encryption").value("TLS"))
                .andExpect(jsonPath("$.fromEmail").value("noreply@example.com"));
    }

    @Test
    void shouldPersistPostedSmtpConfiguration() throws Exception {
        SmtpConfigRequest request = request();
        when(service.saveConfiguration(any())).thenReturn(configuration());

        mockMvc.perform(post("/api/v1/smtp")
                        .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "host":"smtp.example.com",
                                                                    "port":587,
                                                                    "username":"smtp-user",
                                                                    "password":"smtp-password",
                                                                    "encryption":"TLS",
                                                                    "fromEmail":"noreply@example.com",
                                                                    "fromName":"Stentio"
                                                                }
                                                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.example.com"))
                .andExpect(jsonPath("$.port").value(587));

        verify(service).saveConfiguration(eq(request));
    }

    @Test
    void shouldTestPostedSmtpConfiguration() throws Exception {
        when(service.testConnection(any(), eq("destino@example.com")))
                .thenReturn(new SmtpTestResponse(true, "E-mail de teste enviado!"));

        mockMvc.perform(post("/api/v1/smtp/test")
                        .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "host":"smtp.example.com",
                                                                    "port":587,
                                                                    "username":"smtp-user",
                                                                    "password":"smtp-password",
                                                                    "encryption":"TLS",
                                                                    "fromEmail":"noreply@example.com",
                                                                    "fromName":"Stentio",
                                                                    "testEmail":"destino@example.com"
                                                                }
                                                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("E-mail de teste enviado!"));
    }

    private SmtpConfigRequest request() {
        return new SmtpConfigRequest(
                "smtp.example.com",
                587,
                "smtp-user",
                "smtp-password",
                "TLS",
                "noreply@example.com",
                "Stentio"
        );
    }

    private SmtpConfigResponse configuration() {
        return new SmtpConfigResponse(
                "smtp.example.com",
                587,
                "smtp-user",
                "smtp-password",
                "TLS",
                "noreply@example.com",
                "Stentio"
        );
    }
}