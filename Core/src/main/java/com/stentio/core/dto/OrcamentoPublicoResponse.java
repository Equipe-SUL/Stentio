package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItemServico;
import com.stentio.core.model.StatusOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrcamentoPublicoResponse(
        UUID id,
        StatusOrcamento status,
        @JsonProperty("valor_total") BigDecimal valorTotal,
        @JsonProperty("data_expiracao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataExpiracao,
        List<Item> itens) {
    public static OrcamentoPublicoResponse de(Orcamento orcamento) {
        // US-11: os itens passaram a ser pares de idiomas com serviços. O link público mantém a
        // lista plana de serviços, na ordem dos itens, para não quebrar quem já consome este formato.
        List<Item> servicos = orcamento.getItens().stream()
                .flatMap(item -> item.getServicos().stream())
                .map(Item::de)
                .toList();
        return new OrcamentoPublicoResponse(orcamento.getId(), orcamento.getStatus(),
                orcamento.getValorTotal(), orcamento.getDataExpiracao(), servicos);
    }

    public record Item(
            @JsonProperty("tipo_de_serviço") String tipoServico,
            BigDecimal quantidade,
            String unidade,
            @JsonProperty("valor_unitario") BigDecimal valorUnitario,
            BigDecimal subtotal) {
        static Item de(OrcamentoItemServico servico) {
            String unidade = servico.getUnidade() == null ? null : servico.getUnidade().name();
            return new Item(servico.getTipoServico(), servico.getQuantidade(), unidade,
                    servico.getValorUnitario(), servico.getSubtotal());
        }
    }
}
