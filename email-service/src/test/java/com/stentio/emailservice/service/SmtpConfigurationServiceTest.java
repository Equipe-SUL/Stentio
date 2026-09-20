package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SmtpConfigRequest;
import com.stentio.emailservice.dto.SmtpConfigResponse;
import com.stentio.emailservice.repository.SmtpConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpConfigurationServiceTest {

    private final SmtpConfigurationRepository repository = mock(SmtpConfigurationRepository.class);
    private final SmtpConfigurationService service = new SmtpConfigurationService(repository);

    @Test
    void shouldLoadConfigurationFromMongo() {
        when(repository.findById("default")).thenReturn(Optional.of(document()));

        SmtpConfigResponse result = service.getConfiguration();

        assertEquals("smtp.example.com", result.host());
        assertEquals(587, result.port());
        assertEquals("TLS", result.encryption());
    }

    @Test
    void shouldSaveConfigurationInMongo() {
        SmtpConfigRequest request = new SmtpConfigRequest(
                "smtp.example.com", 587, "user", "password", "TLS",
                "noreply@example.com", "Stentio"
        );
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenReturn(document());

        service.saveConfiguration(request);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                "default".equals(saved.getId())
                        && "smtp.example.com".equals(saved.getHost())
                        && saved.getPort() == 587
                        && "TLS".equals(saved.getEncryption())
        ));
    }

    private com.stentio.emailservice.model.SmtpConfigurationDocument document() {
        return new com.stentio.emailservice.model.SmtpConfigurationDocument(
                "default", "smtp.example.com", 587, "user", "password", "TLS",
                "noreply@example.com", "Stentio"
        );
    }
}