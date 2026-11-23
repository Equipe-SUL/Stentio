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
 * US-013 CA.6: avisa a equipe de que uma alocação direta foi recusada.
 *
 * <p>Vai para um endereço de operação configurado, e não para "o gestor da OS", porque a
 * ordem de serviço ainda não guarda quem a conduz. Quando guardar, só este destinatário
 * muda — a etapa recusada já volta para a fila sozinha de qualquer forma.
 */
@Service
public class RecusaEtapaEmailService {

    private static final Logger log = LoggerFactory.getLogger(RecusaEtapaEmailService.class);

    private final NotificacaoEtapaOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final String emailEquipe;

    public RecusaEtapaEmailService(NotificacaoEtapaOutboxRepository outbox,
                                   ObjectMapper objectMapper,
                                   @Value("${app.equipe.email:operacoes@stentio.local}") String emailEquipe) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.emailEquipe = emailEquipe;

        if (emailEquipe.endsWith(".local")) {
            log.warn("app.equipe.email está em {} — avisos de recusa não saem desta máquina", emailEquipe);
        }
    }

    /** Na mesma transação da recusa: ou as duas coisas acontecem, ou nenhuma. */
    public void avisarRecusa(EtapaOrdemServico etapa, Recurso quemRecusou, Instant agora) {
        String codigo = CodigoVisual.gerar("OS", etapa.getOrdemServico().getCriadoEm(),
                etapa.getOrdemServico().getId());

        String corpo = "A alocação direta abaixo foi recusada.\n\n"
                + "Ordem de serviço: " + codigo + '\n'
                + "Etapa: " + etapa.getNome() + '\n'
                + "Recusada por: " + quemRecusou.getNome() + " (" + quemRecusou.getEmail() + ")\n\n"
                + "A etapa voltou para pendente e pode ser alocada a outra pessoa ou disparada "
                + "para todos os elegíveis.\n\nStentio\n";

        outbox.save(new NotificacaoEtapaOutbox(
                etapa,
                emailEquipe,
                objectMapper.writeValueAsString(
                        new NotificacaoEtapaMensagem(emailEquipe, "Etapa recusada: " + etapa.getNome(), corpo)),
                agora));
    }
}
