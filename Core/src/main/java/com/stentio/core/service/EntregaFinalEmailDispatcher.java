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
        EntregaFinalEmailOutbox envio = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (envio == null) {
            return false;
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

        Instant agora = clock.instant();
        envio.setEnviadoEm(agora);
        envio.setPayload(null);

        // CA.8: o encerramento é consequência do ACK.
        //
        // A marcação do envio vem antes e não é desfeita se o encerramento falhar. Depois
        // do ACK a mensagem está no broker de verdade; deixar a transação inteira cair
        // aqui faria a próxima rodada republicar e o cliente receberia o pacote duas
        // vezes. Entre uma OS que ficou sem fechar e um e-mail duplicado, a primeira é
        // visível no painel e corrigível, o segundo não volta atrás.
        try {
            OrdemServico ordem = envio.getOrdemServico();
            StatusOrdemServico anterior = ordem.getStatus();
            ordem.encerrar(agora, envio.getAprovadoPor());
            // US-019: quem encerra é o ACK, fora de requisição; o aprovador vai na observação.
            eventos.publishEvent(EventosAuditoria.ordemServico(ordem, anterior,
                    "Entrega final aprovada por " + envio.getAprovadoPor()
                            + " e enviada ao cliente.", AtorAuditoria.sistema()));
        } catch (RuntimeException ex) {
            log.error("Entrega final da OS {} publicada, mas a OS não pôde ser encerrada: {}",
                    envio.getOrdemServico().getId(), ex.getMessage());
        }

        return true;
    }
}
