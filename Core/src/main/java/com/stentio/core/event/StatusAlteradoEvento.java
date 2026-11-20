package com.stentio.core.event;

import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.support.AtorAuditoria;

import java.util.Objects;
import java.util.UUID;

/**
 * US-019 CA.1: evento de domínio "o status desta entidade mudou".
 *
 * <p>Publicado pelo service que fez a mudança, dentro da transação dela; quem grava o log
 * é o {@code AuditoriaStatusListener}. O service não sabe nada de auditoria além de
 * anunciar o que aconteceu.
 *
 * <p>Os status vão como texto porque cada entidade tem o seu enum. Use as fábricas de
 * {@link EventosAuditoria}, que montam a referência legível do jeito que as telas mostram.
 *
 * @param statusAnterior nulo quando o evento marca a criação da entidade
 * @param ator           nulo para "quem estiver autenticado": o listener resolve pelo JWT.
 *                       Ações sem login (CA.4) precisam informá-lo.
 */
public record StatusAlteradoEvento(
        EntidadeAuditada entidade,
        UUID entidadeId,
        String referenciaLegivel,
        String statusAnterior,
        String statusNovo,
        String observacao,
        AtorAuditoria ator
) {
    public StatusAlteradoEvento {
        Objects.requireNonNull(entidade, "entidade");
        Objects.requireNonNull(entidadeId, "entidadeId");
        Objects.requireNonNull(referenciaLegivel, "referenciaLegivel");
        Objects.requireNonNull(statusNovo, "statusNovo");
    }

    /** Re-publicar o mesmo status (ex.: remontar etapas de uma OS já em planejamento) não é transição. */
    public boolean houveTransicao() {
        return !statusNovo.equals(statusAnterior);
    }
}
