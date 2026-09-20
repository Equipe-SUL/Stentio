package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.SmtpConfigRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class SmtpMailSenderFactory {

    public JavaMailSender create(SmtpConfigRequest config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.host());
        sender.setPort(config.port());
        sender.setUsername(config.username());
        sender.setPassword(config.password());

        Properties properties = sender.getJavaMailProperties();
        boolean authenticated = config.username() != null && !config.username().isBlank()
                && config.password() != null && !config.password().isBlank();
        properties.put("mail.smtp.auth", authenticated);
        properties.put("mail.smtp.starttls.enable", "TLS".equals(config.encryption()));
        properties.put("mail.smtp.starttls.required", "TLS".equals(config.encryption()));
        properties.put("mail.smtp.ssl.enable", "SSL".equals(config.encryption()));
        return sender;
    }
}