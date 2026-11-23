package com.stentio.core.service;

import com.stentio.core.model.NotificacaoEtapaOutbox;
import com.stentio.core.repository.NotificacaoEtapaOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** US-013: tira uma notificação da outbox por vez e publica, com nova tentativa em caso de falha. */
@Service
public class NotificacaoEtapaOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEtapaOutboxDispatcher.class);

    private final NotificacaoEtapaOutboxRepository repository;
    private final NotificacaoEtapaPublisher publisher;
    private final Clock clock;
    private final long retrySegundos;

    public NotificacaoEtapaOutboxDispatcher(NotificacaoEtapaOutboxRepository repository,
                                        NotificacaoEtapaPublisher publisher,
                                        Clock clock,
                                        @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
    }

    @Transactional
    public boolean processarProximo() {
        NotificacaoEtapaOutbox notificacao = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (notificacao == null) {
            return false;
        }

        notificacao.setTentativas(notificacao.getTentativas() + 1);
        try {
            publisher.publicar(notificacao);
            notificacao.setEnviadoEm(clock.instant());
            // O corpo já cumpriu o papel; guardá-lo manteria e-mail de gente na tabela sem motivo.
            notificacao.setPayload(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            notificacao.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            notificacao.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            log.warn("Notificação da etapa {} pendente; nova tentativa agendada ({})",
                    notificacao.getEtapa().getId(), ex.getClass().getSimpleName());
        }
        return true;
    }
}
