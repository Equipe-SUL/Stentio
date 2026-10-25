package com.stentio.emailservice;

import com.stentio.emailservice.consumer.EmailConsumer;
import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.SendEmailRequest;
import com.stentio.emailservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailWorkflowTests {

    @Test
    void enviaEmailComOsCamposObrigatorios() throws Exception {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailService service = new EmailService(mailSender);

        service.send(new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto do email",
                "Mensagem de teste"
        ));

        assertEquals(1, mailSender.enviadas.size());
        MimeMessage mensagem = mailSender.enviadas.get(0);
        assertEquals("destinatario@teste.com", mensagem.getAllRecipients()[0].toString());
        assertEquals("Assunto do email", mensagem.getSubject());
        assertTrue(mime(mensagem).contains("Mensagem de teste"));
    }

    @Test
    void recusaRequisicaoSemCamposObrigatorios() {
        EmailService service = new EmailService(new RecordingMailSender());

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("", "Assunto", "Mensagem"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("destinatario@teste.com", "", "Mensagem"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.send(new SendEmailRequest("destinatario@teste.com", "Assunto", ""))
        );
    }

    // US-13: o corpo HTML e os anexos entram na mesma mensagem MIME.
    @Test
    void enviaMensagemComHtmlAnexoEImagemEmbutida() throws Exception {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailService service = new EmailService(mailSender);

        service.enviar(new MensagemEmail(
                "cliente@teste.com",
                "Orcamento ORC-2026-ABCDEF",
                "Versao em texto puro",
                "<p>Versao em HTML</p>",
                List.of(new MensagemEmail.Anexo("contrato.pdf",
                        "conteudo do pdf".getBytes(StandardCharsets.UTF_8), "application/pdf")),
                new MensagemEmail.ImagemInline("logoEmpresa",
                        "bytes da logo".getBytes(StandardCharsets.UTF_8), "image/png")));

        String mime = mime(mailSender.enviadas.get(0));
        assertTrue(mime.contains("Versao em texto puro"));
        assertTrue(mime.contains("Versao em HTML"));
        assertTrue(mime.contains("contrato.pdf"));
        assertTrue(mime.contains("logoEmpresa"));
        assertTrue(mime.contains("application/pdf"));
    }

    @Test
    void consumidorPropagaFalhaDeEnvioParaQueORabbitNaoConfirmeAMensagem() {
        EmailConsumer consumer = new EmailConsumer(new EmailService(new FailingMailSender()));

        assertThrows(MailException.class, () -> consumer.consume(new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto",
                "Mensagem"
        )));
    }

    @Test
    void consumidorEnviaOEmailDaFila() {
        RecordingMailSender mailSender = new RecordingMailSender();
        EmailConsumer consumer = new EmailConsumer(new EmailService(mailSender));

        consumer.consume(new SendEmailRequest(
                "destinatario@teste.com",
                "Assunto",
                "Mensagem"
        ));

        assertEquals(1, mailSender.enviadas.size());
    }

    private static String mime(MimeMessage mensagem) throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        mensagem.writeTo(saida);
        return saida.toString(StandardCharsets.UTF_8);
    }

    // MimeMessage precisa de uma Session real para ser montada, então o sender de teste
    // delega a criação ao JavaMailSenderImpl e só guarda o que seria enviado.
    private static class RecordingMailSender implements JavaMailSender {
        private final JavaMailSenderImpl delegate = new JavaMailSenderImpl();
        private final List<MimeMessage> enviadas = new ArrayList<>();

        @Override
        public MimeMessage createMimeMessage() {
            return delegate.createMimeMessage();
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            return delegate.createMimeMessage(contentStream);
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            enviadas.add(mimeMessage);
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            for (MimeMessage mimeMessage : mimeMessages) {
                enviadas.add(mimeMessage);
            }
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
        }
    }

    private static class FailingMailSender implements JavaMailSender {
        private final JavaMailSenderImpl delegate = new JavaMailSenderImpl();

        @Override
        public MimeMessage createMimeMessage() {
            return delegate.createMimeMessage();
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            return delegate.createMimeMessage(contentStream);
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            throw new MailException("Simulated send failure") {
            };
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            throw new MailException("Simulated send failure") {
            };
        }
    }
}
