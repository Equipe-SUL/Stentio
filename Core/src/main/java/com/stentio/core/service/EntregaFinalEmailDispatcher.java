package com.stentio.core.service;

import com.stentio.core.event.EventosAuditoria;
import com.stentio.core.model.EntregaFinalEmailOutbox;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.support.AtorAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * US-015 CA.8 e CA.9: publica o pacote e, só com o ACK na mão, encerra a OS.
 *
 * <p>Este dispatcher faz mais do que os outros dois: além de marcar o envio, ele muda o
 * estado de negócio da ordem de serviço. É de propósito — a CA.8 define o encerramento
 * como consequência do ACK, e qualquer outro lugar para essa transição deixaria uma
 * janela em que a OS está fechada sem a mensagem ter saído.
 *
 * <p>Falha de publicação não encerra nada: a linha volta para a fila com nova tentativa
 * agendada e a OS continua aguardando verificação (CA.9).
 */
@Service
public class EntregaFinalEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EntregaFinalEmailDispatcher.class);

    private final EntregaFinalEmailOutboxRepository repository;
    private final EntregaFinalEmailPublisher publisher;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;
    private final long retrySegundos;

    public EntregaFinalEmailDispatcher(EntregaFinalEmailOutboxRepository repository,
            EntregaFinalEmailPublisher publisher, ApplicationEventPublisher eventos, Clock clock,
            @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos) {
        this.repository = repository;
        this.publisher = publisher;
        this.eventos = eventos;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
    }

    @Transactional
    public boolean processarProximo() {
        Instant agora = clock.instant();
        EntregaFinalEmailOutbox envio = repository.buscarPendenteComLock(agora).orElse(null);
        if (envio == null) {
            return repository.buscarPublicadaSemEncerramentoComLock(agora)
                    .map(publicada -> {
                        encerrar(publicada, agora);
                        return true;
                    })
                    .orElse(false);
        }

        envio.setTentativas(envio.getTentativas() + 1);
        try {
            publisher.publicar(envio);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            envio.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            envio.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            log.warn("Entrega final da OS {} pendente; nova tentativa agendada ({})",
                    envio.getOrdemServico().getId(), ex.getClass().getSimpleName());
            return true;
        }

        Instant enviadoEm = clock.instant();
        envio.setEnviadoEm(enviadoEm);
        envio.setPayload(null);
        encerrar(envio, enviadoEm);
        return true;
    }

    private void encerrar(EntregaFinalEmailOutbox envio, Instant agora) {
        OrdemServico ordem = envio.getOrdemServico();
        if (ordem.getStatus() == StatusOrdemServico.ENCERRADA) {
            envio.setEncerradoEm(agora);
            return;
        }

        try {
            StatusOrdemServico anterior = ordem.getStatus();
            ordem.encerrar(agora, envio.getAprovadoPor());
            eventos.publishEvent(EventosAuditoria.ordemServico(ordem, anterior,
                    "Entrega final aprovada por " + envio.getAprovadoPor()
                            + " e enviada ao cliente.", AtorAuditoria.sistema()));
            envio.setEncerradoEm(agora);
        } catch (RuntimeException ex) {
            envio.setProximaTentativaEm(agora.plusSeconds(retrySegundos));
            log.error("Entrega final da OS {} publicada, mas a OS não pôde ser encerrada: {}",
                    ordem.getId(), ex.getMessage());
        }
    }
}
