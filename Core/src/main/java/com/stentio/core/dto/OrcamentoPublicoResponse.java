package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItem;
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
        return new OrcamentoPublicoResponse(orcamento.getId(), orcamento.getStatus(),
                orcamento.getValorTotal(), orcamento.getDataExpiracao(),
                orcamento.getItens().stream().map(Item::de).toList());
    }

    public record Item(
            @JsonProperty("tipo_de_serviço") String tipoServico,
            BigDecimal quantidade,
            String unidade,
            @JsonProperty("valor_unitario") BigDecimal valorUnitario,
            BigDecimal subtotal) {
        static Item de(OrcamentoItem item) {
            return new Item(item.getTipoServico(), item.getQuantidade(), item.getUnidade(),
                    item.getValorUnitario(), item.getSubtotal());
        }
    }
}
