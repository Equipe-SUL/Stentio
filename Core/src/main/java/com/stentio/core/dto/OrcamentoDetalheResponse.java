package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.UnidadeCobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// US-11: detalhe do orçamento com a hierarquia completa (CA.6). Também é o corpo dos POSTs.
public record OrcamentoDetalheResponse(
        UUID id,
        // Versão de negócio (numero_versao), não o contador do lock otimista.
        int versao,
        @JsonProperty("cliente_id") UUID clienteId,
        @JsonProperty("solicitacao_id") UUID solicitacaoId,
        StatusOrcamento status,
        @JsonProperty("valor_total") BigDecimal valorTotal,
        @JsonProperty("data_expiracao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataExpiracao,
        List<Item> itens
) {

    public static OrcamentoDetalheResponse de(Orcamento orcamento) {
        return new OrcamentoDetalheResponse(
                orcamento.getId(),
                orcamento.getNumeroVersao(),
                orcamento.getCliente().getId(),
                orcamento.getSolicitacao() == null ? null : orcamento.getSolicitacao().getId(),
                orcamento.getStatus(),
                orcamento.getValorTotal(),
                orcamento.getDataExpiracao(),
                orcamento.getItens().stream().map(Item::de).toList());
    }

    public record Item(
            UUID id,
            int ordem,
            @JsonProperty("idioma_origem_id") UUID idiomaOrigemId,
            @JsonProperty("idioma_destino_id") UUID idiomaDestinoId,
            @JsonProperty("subtotal_item") BigDecimal subtotalItem,
            List<Servico> servicos
    ) {
        static Item de(OrcamentoItem item) {
            return new Item(
                    item.getId(),
                    item.getOrdem(),
                    item.getIdiomaOrigem().getId(),
                    item.getIdiomaDestino().getId(),
                    item.getSubtotalItem(),
                    item.getServicos().stream().map(Servico::de).toList());
        }
    }

    public record Servico(
            UUID id,
            @JsonProperty("tipo_servico") String tipoServico,
            BigDecimal quantidade,
            UnidadeCobranca unidade,
            @JsonProperty("valor_unitario") BigDecimal valorUnitario,
            BigDecimal subtotal
    ) {
        static Servico de(OrcamentoItemServico servico) {
            return new Servico(
                    servico.getId(),
                    servico.getTipoServico(),
                    servico.getQuantidade(),
                    servico.getUnidade(),
                    servico.getValorUnitario(),
                    servico.getSubtotal());
        }
    }
}
