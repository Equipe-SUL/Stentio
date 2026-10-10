package com.stentio.core.config;

import com.stentio.core.service.OrcamentoOutboxDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.orcamentos.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OrcamentoOutboxScheduler {
    private final OrcamentoOutboxDispatcher dispatcher;

    public OrcamentoOutboxScheduler(OrcamentoOutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.orcamentos.outbox.intervalo-ms:5000}")
    public void publicarPendentes() {
        // Limita o trabalho por rodada, sem carregar a tabela inteira em memória.
        for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
            if (!dispatcher.processarProximo()) break;
        }
    }
}
