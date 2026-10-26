package com.stentio.core.service;

import com.stentio.core.model.OrcamentoDecisaoOutbox;
import com.stentio.core.repository.OrcamentoDecisaoOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class OrcamentoOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OrcamentoOutboxDispatcher.class);
    private final OrcamentoDecisaoOutboxRepository repository;
    private final OrcamentoEventoPublisher publisher;
    private final Clock clock;
    private final long retrySegundos;

    public OrcamentoOutboxDispatcher(OrcamentoDecisaoOutboxRepository repository,
            OrcamentoEventoPublisher publisher, Clock clock,
            @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
    }

    // Uma transação por mensagem, chamada por outro bean para ativar o proxy transacional.
    @Transactional
    public boolean processarProximo() {
        OrcamentoDecisaoOutbox evento = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (evento == null) return false;
        evento.setTentativas(evento.getTentativas() + 1);
        try {
            publisher.publicar(evento);
            evento.setPublicadoEm(clock.instant());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            evento.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            evento.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            // IDs e classe da falha são suficientes; não expõe motivo, payload ou token.
            log.warn("Evento de orçamento {} pendente; nova tentativa agendada ({})",
                    evento.getId(), ex.getClass().getSimpleName());
        }
        return true;
    }
}
