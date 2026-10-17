package com.stentio.core.service;

import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.repository.OrcamentoEmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class OrcamentoEmailOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OrcamentoEmailOutboxDispatcher.class);
    private final OrcamentoEmailOutboxRepository repository;
    private final OrcamentoEmailPublisher publisher;
    private final Clock clock;
    private final long retrySegundos;

    public OrcamentoEmailOutboxDispatcher(OrcamentoEmailOutboxRepository repository,
            OrcamentoEmailPublisher publisher, Clock clock,
            @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
    }

    @Transactional
    public boolean processarProximo() {
        OrcamentoEmailOutbox email = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (email == null) return false;
        email.setTentativas(email.getTentativas() + 1);
        try {
            publisher.publicar(email);
            email.setEnviadoEm(clock.instant());
            email.setPayload(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            email.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            email.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            log.warn("E-mail do orçamento {} pendente; nova tentativa agendada ({})",
                    email.getOrcamento().getId(), ex.getClass().getSimpleName());
        }
        return true;
    }
}
