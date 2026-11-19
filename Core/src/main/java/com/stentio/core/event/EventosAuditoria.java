package com.stentio.core.event;

import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.StatusOrdemServico;
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
     * @param anterior nulo quando a OS acaba de nascer da aprovação do orçamento
     */
    public static StatusAlteradoEvento ordemServico(OrdemServico ordem, StatusOrdemServico anterior,
                                                    String observacao, AtorAuditoria ator) {
        return new StatusAlteradoEvento(
                EntidadeAuditada.ORDEM_SERVICO,
                ordem.getId(),
                "OS " + codigoOs(ordem),
                nome(anterior),
                ordem.getStatus().name(),
                observacao,
                ator);
    }

    /**
     * O status auditado da etapa é o que a API mostra ({@link #statusEtapa}): a alocação
     * aparece como ALOCADO, igual na tela de workflow e no contrato da US.
     *
     * @param anterior nulo quando a etapa acaba de ser criada na sequência
     */
    public static StatusAlteradoEvento etapa(EtapaOrdemServico etapa, String anterior,
                                             String observacao, AtorAuditoria ator) {
        return new StatusAlteradoEvento(
                EntidadeAuditada.ORDEM_SERVICO_ETAPA,
                etapa.getId(),
                "OS %s - Etapa %d (%s)".formatted(codigoOs(etapa.getOrdemServico()), etapa.getOrdem(), etapa.getNome()),
                anterior,
                statusEtapa(etapa),
                observacao,
                ator);
    }

    /**
     * Único ponto que decide o que é "o status" de uma etapa para a auditoria. Hoje a
     * alocação não fica gravada na coluna status (ver {@code SituacaoEtapa}); quando a
     * US-013 transformar ALOCADO em status gravado, basta trocar por {@code getStatus()}.
     */
    public static String statusEtapa(EtapaOrdemServico etapa) {
        return etapa.situacao().name();
    }

    private static String codigoOs(OrdemServico ordem) {
        return CodigoVisual.gerar("OS", criadoEm(ordem.getCriadoEm()), ordem.getId());
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
