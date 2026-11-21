package com.stentio.core.service;

import com.stentio.core.dto.NotificacaoEtapaMensagem;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.NotificacaoEtapaOutbox;
import com.stentio.core.model.Recurso;
import com.stentio.core.repository.NotificacaoEtapaOutboxRepository;
import com.stentio.core.support.CodigoVisual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * US-015 CA.10: avisa o profissional de que a entrega dele voltou para correção.
 *
 * <p>Passa pela mesma outbox de notificação de etapa da US-013: é um e-mail sobre uma
 * etapa, enfileirado na transação que mudou essa etapa. O portal também mostra a demanda
 * de volta, mas quem já entregou não fica olhando o portal — o aviso precisa ir atrás.
 */
@Service
public class ReaberturaEtapaEmailService {

    private static final Logger log = LoggerFactory.getLogger(ReaberturaEtapaEmailService.class);

    private final NotificacaoEtapaOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final String urlPortal;

    public ReaberturaEtapaEmailService(NotificacaoEtapaOutboxRepository outbox,
                                       ObjectMapper objectMapper,
                                       @Value("${app.portal-recurso.url:http://localhost/recurso/demandas}") String urlPortal) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.urlPortal = urlPortal;
    }

    /** Na mesma transação da reprovação: ou as duas coisas acontecem, ou nenhuma. */
    public void avisarReabertura(EtapaOrdemServico etapa, String motivo, Instant agora) {
        Recurso responsavel = etapa.getRecurso();
        if (responsavel == null) {
            // Não deveria acontecer: a reabertura mantém o recurso. Se acontecer, a
            // reprovação em si não pode cair por causa do aviso.
            log.warn("Etapa {} reaberta sem recurso: ninguém foi avisado", etapa.getId());
            return;
        }

        String codigo = CodigoVisual.gerar("OS", etapa.getOrdemServico().getCriadoEm(),
                etapa.getOrdemServico().getId());
        String assunto = "Correção solicitada: " + etapa.getNome();

        String corpo = "Olá, " + responsavel.getNome() + ".\n\n"
                + "A entrega da etapa abaixo foi revisada e precisa de ajuste.\n\n"
                + "Ordem de serviço: " + codigo + '\n'
                + "Etapa: " + etapa.getNome() + "\n\n"
                + "Motivo informado pela equipe:\n" + motivo + "\n\n"
                + "A etapa voltou para a sua lista com o mesmo prazo. Para enviar a correção, "
                + "acesse o portal:\n" + urlPortal + "\n\nEquipe Aliança Traduções\n";

        outbox.save(new NotificacaoEtapaOutbox(
                etapa,
                responsavel.getEmail(),
                objectMapper.writeValueAsString(
                        new NotificacaoEtapaMensagem(responsavel.getEmail(), assunto, corpo)),
                agora));
    }
}
