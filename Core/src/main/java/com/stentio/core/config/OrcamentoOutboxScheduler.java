package com.stentio.core.config;

import com.stentio.core.service.NotificacaoEtapaOutboxDispatcher;
import com.stentio.core.service.OrcamentoEmailOutboxDispatcher;
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
    private final OrcamentoEmailOutboxDispatcher emailDispatcher;
    private final NotificacaoEtapaOutboxDispatcher notificacaoDispatcher;

    public OrcamentoOutboxScheduler(OrcamentoOutboxDispatcher dispatcher,
                                    OrcamentoEmailOutboxDispatcher emailDispatcher,
                                    NotificacaoEtapaOutboxDispatcher notificacaoDispatcher) {
        this.dispatcher = dispatcher;
        this.emailDispatcher = emailDispatcher;
        this.notificacaoDispatcher = notificacaoDispatcher;
    }

    @Scheduled(fixedDelayString = "${app.orcamentos.outbox.intervalo-ms:5000}")
    public void publicarPendentes() {
        // Limita o trabalho por rodada, sem carregar a tabela inteira em memória.
        for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
            if (!dispatcher.processarProximo()) break;
        }
        for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
            if (!emailDispatcher.processarProximo()) break;
        }
        // US-013: um disparo gera um convite por elegível, então esta fila cresce mais
        // depressa que as outras duas em uma rodada só.
        for (int i = 0; i < 50 && !Thread.currentThread().isInterrupted(); i++) {
            if (!notificacaoDispatcher.processarProximo()) break;
        }
    }
}
