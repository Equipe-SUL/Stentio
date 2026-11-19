package com.stentio.core.event;

import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.support.AtorAuditoria;
import com.stentio.core.support.CodigoVisual;

import java.time.Instant;

/**
 * US-019: fábricas de {@link StatusAlteradoEvento}, uma por entidade auditada.
 *
 * <p>Concentram a montagem da {@code referencia_legivel} no mesmo formato de código que as
 * telas exibem (ORC-2026-A1B2C3, OS-2026-…), para o administrador conseguir buscar no log
 * pelo código que ele vê no resto do sistema.
 */
public final class EventosAuditoria {

    private EventosAuditoria() {
    }

    /**
     * @param anterior nulo na criação do orçamento
     * @param ator     nulo para usar o usuário autenticado
     */
    public static StatusAlteradoEvento orcamento(Orcamento orcamento, StatusOrcamento anterior,
                                                 String observacao, AtorAuditoria ator) {
        return new StatusAlteradoEvento(
                EntidadeAuditada.ORCAMENTO,
                orcamento.getId(),
                "Orçamento " + CodigoVisual.gerar("ORC", criadoEm(orcamento.getCriadoEm()), orcamento.getId()),
                nome(anterior),
                orcamento.getStatus().name(),
                observacao,
                ator);
    }

    /**
     * Entidade recém-persistida ainda não passou pelo flush, e o {@code @CreationTimestamp}
     * só é preenchido no INSERT. O ano de agora é o mesmo que o INSERT vai gravar instantes depois.
     */
    static Instant criadoEm(Instant criadoEm) {
        return criadoEm != null ? criadoEm : Instant.now();
    }

    static String nome(Enum<?> status) {
        return status == null ? null : status.name();
    }
}
