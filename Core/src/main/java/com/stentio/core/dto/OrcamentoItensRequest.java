package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.UnidadeCobranca;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// US-11: POST /api/v1/orcamentos/{id}/itens. A lista enviada substitui todos os itens atuais.
// Subtotais não fazem parte do corpo: são sempre calculados no servidor.
public record OrcamentoItensRequest(
        @NotEmpty(message = "Informe ao menos um item")
        List<@NotNull(message = "Item não pode ser nulo") @Valid Item> itens
) {

    public record Item(
            @NotNull(message = "Informe a ordem do item")
            @Positive(message = "A ordem do item deve ser maior que zero")
            Integer ordem,

            // CA.2: o item exige idioma de origem e de destino.
            @JsonProperty("idioma_origem_id")
            @NotNull(message = "Informe o idioma de origem")
            UUID idiomaOrigemId,

            @JsonProperty("idioma_destino_id")
            @NotNull(message = "Informe o idioma de destino")
            UUID idiomaDestinoId,

            // RN.3
            @NotEmpty(message = "Todo item deve conter ao menos um serviço precificado")
            List<@NotNull(message = "Serviço não pode ser nulo") @Valid Servico> servicos
    ) {
    }

    public record Servico(
            @JsonProperty("tipo_servico")
            @NotBlank(message = "Informe o tipo de serviço")
            @Size(max = 150, message = "O tipo de serviço deve ter no máximo 150 caracteres")
            String tipoServico,

            // RN.4. Limites de dígitos iguais às colunas NUMERIC(14,2) e NUMERIC(14,4).
            @NotNull(message = "Informe a quantidade")
            @Positive(message = "A quantidade deve ser maior que zero")
            @Digits(integer = 12, fraction = 2, message = "A quantidade aceita até 12 dígitos inteiros e 2 decimais")
            BigDecimal quantidade,

            @NotNull(message = "Informe a unidade")
            UnidadeCobranca unidade,

            @JsonProperty("valor_unitario")
            @NotNull(message = "Informe o valor unitário")
            @Positive(message = "O valor unitário deve ser maior que zero")
            @Digits(integer = 10, fraction = 4, message = "O valor unitário aceita até 10 dígitos inteiros e 4 decimais")
            BigDecimal valorUnitario
    ) {
    }
}
