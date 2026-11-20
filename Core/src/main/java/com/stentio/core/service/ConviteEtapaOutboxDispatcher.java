package com.stentio.core.service;

import com.stentio.core.model.ConviteEtapaOutbox;
import com.stentio.core.repository.ConviteEtapaOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** US-013 CA.2: tira um convite da outbox por vez e publica, com nova tentativa em caso de falha. */
@Service
public class ConviteEtapaOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ConviteEtapaOutboxDispatcher.class);

    private final ConviteEtapaOutboxRepository repository;
    private final ConviteEtapaPublisher publisher;
    private final Clock clock;
    private final long retrySegundos;

    public ConviteEtapaOutboxDispatcher(ConviteEtapaOutboxRepository repository,
                                        ConviteEtapaPublisher publisher,
                                        Clock clock,
                                        @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
    }

    @Transactional
    public boolean processarProximo() {
        ConviteEtapaOutbox convite = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (convite == null) {
            return false;
        }

        convite.setTentativas(convite.getTentativas() + 1);
        try {
            publisher.publicar(convite);
            convite.setEnviadoEm(clock.instant());
            // O corpo já cumpriu o papel; guardá-lo manteria e-mail de gente na tabela sem motivo.
            convite.setPayload(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            convite.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            convite.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            log.warn("Convite da etapa {} pendente; nova tentativa agendada ({})",
                    convite.getEtapa().getId(), ex.getClass().getSimpleName());
        }
        return true;
    }
}
