package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SmtpConfigRequest;
import com.stentio.emailservice.dto.SmtpConfigResponse;
import com.stentio.emailservice.dto.SmtpTestResponse;
import com.stentio.emailservice.model.SmtpConfigurationDocument;
import com.stentio.emailservice.repository.SmtpConfigurationRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class SmtpConfigurationService {

    private static final String DEFAULT_ID = "default";

    private final SmtpConfigurationRepository repository;
    private final SmtpMailSenderFactory mailSenderFactory;

    public SmtpConfigurationService(SmtpConfigurationRepository repository) {
        this(repository, new SmtpMailSenderFactory());
    }

    @Autowired
    public SmtpConfigurationService(
            SmtpConfigurationRepository repository,
            SmtpMailSenderFactory mailSenderFactory
    ) {
        this.repository = repository;
        this.mailSenderFactory = mailSenderFactory;
    }

    public SmtpConfigResponse getConfiguration() {
        return repository.findById(DEFAULT_ID)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(defaultConfiguration()));
    }

    public SmtpConfigResponse saveConfiguration(SmtpConfigRequest request) {
        SmtpConfigurationDocument saved = repository.save(toDocument(request));
        return toResponse(saved);
    }

    public SmtpTestResponse testConnection(SmtpConfigRequest config, String testEmail) {
        try {
            JavaMailSender sender = mailSenderFactory.create(config);
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            message.setTo(testEmail);
            message.setSubject("Stentio - Teste de Conexão SMTP");
            message.setText("E-mail de teste enviado pelo serviço Stentio.");
            if (config.fromEmail() != null && !config.fromEmail().isBlank()) {
                message.setFrom(buildFromAddress(config.fromEmail(), config.fromName()));
            }
            sender.send(mimeMessage);
            return new SmtpTestResponse(true, "E-mail de teste enviado!");
        } catch (Exception exception) {
            return new SmtpTestResponse(false, "Falha ao conectar ao servidor SMTP: " + exception.getMessage());
        }
    }

    private InternetAddress buildFromAddress(String email, String name) {
        try {
            if (name != null && !name.isBlank()) {
                return new InternetAddress(email, name, "UTF-8");
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        try {
            return new InternetAddress(email);
        } catch (AddressException exception) {
            throw new IllegalArgumentException("E-mail do remetente inválido: " + email, exception);
        }
    }

    public SmtpConfigRequest getStoredConfiguration() {
        return repository.findById(DEFAULT_ID)
                .map(this::toRequest)
                .orElseGet(this::defaultConfigurationRequest);
    }

    private SmtpConfigurationDocument toDocument(SmtpConfigRequest request) {
        return new SmtpConfigurationDocument(
                DEFAULT_ID,
                request.host(),
                request.port(),
                request.username(),
                request.password(),
                request.encryption(),
                request.fromEmail(),
                request.fromName()
        );
    }

    private SmtpConfigResponse toResponse(SmtpConfigurationDocument document) {
        return new SmtpConfigResponse(
                document.getHost(), document.getPort(), document.getUsername(),
                document.getPassword(), document.getEncryption(), document.getFromEmail(),
                document.getFromName()
        );
    }

    private SmtpConfigRequest toRequest(SmtpConfigurationDocument document) {
        return new SmtpConfigRequest(
                document.getHost(), document.getPort(), document.getUsername(),
                document.getPassword(), document.getEncryption(), document.getFromEmail(),
                document.getFromName()
        );
    }

    private SmtpConfigurationDocument defaultConfiguration() {
        return new SmtpConfigurationDocument(
                DEFAULT_ID, "localhost", 1025, "", "", "NONE", "", "Stentio"
        );
    }

    private SmtpConfigRequest defaultConfigurationRequest() {
        return new SmtpConfigRequest("localhost", 1025, "", "", "NONE", "", "Stentio");
    }
}