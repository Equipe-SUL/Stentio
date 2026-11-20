package com.stentio.core.service;

import com.stentio.core.event.StatusAlteradoEvento;
import com.stentio.core.model.LogAuditoria;
import com.stentio.core.repository.LogAuditoriaRepository;
import com.stentio.core.support.AtorAuditoria;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;

/**
 * US-019 CA.1: transforma cada {@link StatusAlteradoEvento} em uma linha de {@code log_auditoria}.
 *
 * <p>{@code BEFORE_COMMIT} grava o log na mesma transação da mudança de status: se ela for
 * desfeita, o log também é; se o log falhar, a mudança não é confirmada. Em
 * {@code AFTER_COMMIT} poderíamos ter status alterado sem rastro — justamente o que a
 * auditoria existe para impedir.
 *
 * <p>O listener roda na mesma thread da requisição, então o SecurityContext ainda está lá
 * para resolver o ator quando o evento não trouxe um. Fora de requisição (consumidor do
 * RabbitMQ, agendamentos) não há autenticação, e o ator vira o próprio sistema.
 *
 * <p>Evento publicado fora de transação é descartado (comportamento padrão do
 * {@code @TransactionalEventListener}): todos os pontos que mudam status são transacionais.
 */
@Component
public class AuditoriaStatusListener {

    private final LogAuditoriaRepository logs;
    private final Clock clock;

    public AuditoriaStatusListener(LogAuditoriaRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void registrar(StatusAlteradoEvento evento) {
        if (!evento.houveTransicao()) {
            return;
        }

        AtorAuditoria ator = evento.ator() != null
                ? evento.ator()
                : AtorAuditoria.deAutenticacao(SecurityContextHolder.getContext().getAuthentication());

        logs.save(new LogAuditoria(
                evento.entidade(),
                evento.entidadeId(),
                evento.referenciaLegivel(),
                evento.statusAnterior(),
                evento.statusNovo(),
                ator.usuarioId(),
                ator.nome(),
                ator.perfil(),
                ator.origem(),
                evento.observacao(),
                clock.instant()));
    }
}
