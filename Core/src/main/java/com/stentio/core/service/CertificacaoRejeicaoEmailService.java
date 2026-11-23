package com.stentio.core.service;

import com.stentio.core.dto.NotificacaoEtapaMensagem;
import com.stentio.core.model.CertificacaoEmailOutbox;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.repository.CertificacaoEmailOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * US-024 CA.5: avisa o profissional de que o documento precisa ser reenviado.
 *
 * <p>Vai pela email.queue genérica, que leva assunto e corpo já prontos — não há anexo
 * nem dado estruturado para o email-service renderizar, então fila nova seria sobra.
 */
@Service
public class CertificacaoRejeicaoEmailService {

    private final CertificacaoEmailOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final String urlPortal;

    public CertificacaoRejeicaoEmailService(CertificacaoEmailOutboxRepository outbox,
                                            ObjectMapper objectMapper,
                                            @Value("${app.portal-recurso.url:http://localhost/portal}") String urlPortal) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.urlPortal = urlPortal;
    }

    /** Na mesma transação da rejeição: ou as duas coisas valem, ou nenhuma. */
    public void avisarRejeicao(RecursoCertificacao certificacao, String motivo, Instant agora) {
        Recurso recurso = certificacao.getRecurso();
        String par = certificacao.getIdiomaOrigem().getNome()
                + " para " + certificacao.getIdiomaDestino().getNome();

        String corpo = "Olá, " + recurso.getNome() + ".\n\n"
                + "O documento que você enviou para o par " + par + " não pôde ser aceito.\n\n"
                + "Motivo informado pela equipe:\n" + motivo + "\n\n"
                + "Você pode enviar um novo documento pelo portal:\n" + urlPortal
                + "\n\nEnquanto a certificação não for aprovada, você não recebe etapas deste par.\n\n"
                + "Equipe Aliança Traduções\n";

        outbox.save(new CertificacaoEmailOutbox(
                certificacao,
                recurso.getEmail(),
                objectMapper.writeValueAsString(new NotificacaoEtapaMensagem(
                        recurso.getEmail(), "Certificação não aprovada: " + par, corpo)),
                agora));
    }
}
