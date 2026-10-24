package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.SendEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Transporte: resolve o remetente configurado, monta a mensagem MIME e envia.
 * Não conhece regra de nenhum fluxo — quem monta o conteúdo entrega um {@link MensagemEmail}.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SmtpConfigurationService configurationService;
    private final SmtpMailSenderFactory mailSenderFactory;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.configurationService = null;
        this.mailSenderFactory = null;
    }

    @Autowired
    public EmailService(
            SmtpConfigurationService configurationService,
            SmtpMailSenderFactory mailSenderFactory
    ) {
        this.mailSender = null;
        this.configurationService = configurationService;
        this.mailSenderFactory = mailSenderFactory;
    }

    public void send(SendEmailRequest request) {
        if (request == null || request.to() == null || request.to().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.body() == null || request.body().isBlank()) {
            throw new IllegalArgumentException("Os campos to, subject e body são obrigatórios");
        }
        enviar(MensagemEmail.textoSimples(request.to(), request.subject(), request.body()));
    }

    public void enviar(MensagemEmail mensagem) {
        if (mensagem == null || mensagem.destinatario() == null || mensagem.destinatario().isBlank()
                || mensagem.assunto() == null || mensagem.assunto().isBlank()
                || mensagem.texto() == null || mensagem.texto().isBlank()) {
            throw new IllegalArgumentException("Destinatário, assunto e texto são obrigatórios");
        }

        JavaMailSender sender = resolverSender();
        MimeMessage mimeMessage = sender.createMimeMessage();

        try {
            // MIXED_RELATED é o modo que aceita anexos e imagem embutida na mesma mensagem.
            // Sem nada disso a mensagem segue simples, como antes da US-13.
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, modoMultipart(mensagem), "UTF-8");
            message.setTo(mensagem.destinatario());
            message.setSubject(mensagem.assunto());

            if (mensagem.html() != null && !mensagem.html().isBlank()) {
                // Texto primeiro, HTML depois: é a ordem que o cliente de e-mail usa para
                // escolher a melhor parte que sabe exibir.
                message.setText(mensagem.texto(), mensagem.html());
            } else {
                message.setText(mensagem.texto());
            }

            // addInline só vale depois do setText, senão a imagem não é referenciada pelo HTML.
            MensagemEmail.ImagemInline imagem = mensagem.imagemInline();
            if (imagem != null && imagem.conteudo() != null && imagem.conteudo().length > 0) {
                message.addInline(imagem.cid(), new ByteArrayResource(imagem.conteudo()),
                        tipoOuPadrao(imagem.tipoConteudo()));
            }
            for (MensagemEmail.Anexo anexo : mensagem.anexos()) {
                message.addAttachment(anexo.nome(), new ByteArrayResource(anexo.conteudo()),
                        tipoOuPadrao(anexo.tipoConteudo()));
            }

            aplicarRemetente(message);
        } catch (MessagingException exception) {
            throw new MailPreparationException("Falha ao montar a mensagem de e-mail", exception);
        }

        sender.send(mimeMessage);
    }

    private JavaMailSender resolverSender() {
        return mailSender != null
                ? mailSender
                : mailSenderFactory.create(configurationService.getStoredConfiguration());
    }

    private void aplicarRemetente(MimeMessageHelper message) throws MessagingException {
        if (configurationService == null) {
            return;
        }
        var configuration = configurationService.getConfiguration();
        if (configuration.fromEmail() != null && !configuration.fromEmail().isBlank()) {
            message.setFrom(buildFromAddress(configuration.fromEmail(), configuration.fromName()));
        }
    }

    private static int modoMultipart(MensagemEmail mensagem) {
        boolean precisaMultipart = (mensagem.html() != null && !mensagem.html().isBlank())
                || !mensagem.anexos().isEmpty()
                || mensagem.imagemInline() != null;
        return precisaMultipart
                ? MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED
                : MimeMessageHelper.MULTIPART_MODE_NO;
    }

    private static String tipoOuPadrao(String tipoConteudo) {
        return tipoConteudo == null || tipoConteudo.isBlank()
                ? "application/octet-stream"
                : tipoConteudo;
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
}
