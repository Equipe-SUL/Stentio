package com.stentio.core.dto;

import com.stentio.core.model.UnidadeCobranca;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TabelaPrecoRequest(
        @NotNull(message = "O tipo de serviço é obrigatório")
        UUID tipoServicoId,

        @NotNull(message = "O idioma de origem é obrigatório")
        UUID idiomaOrigemId,

        @NotNull(message = "O idioma de destino é obrigatório")
        UUID idiomaDestinoId,

        @NotNull(message = "A unidade de cobrança é obrigatória")
        UnidadeCobranca unidade,

        // RN.2 e Cenário 3
        @NotNull(message = "O valor unitário é obrigatório")
        @Positive(message = "O valor unitário deve ser maior que zero")
        @Digits(integer = 8, fraction = 4, message = "O valor unitário deve ter no máximo 8 dígitos inteiros e 4 casas decimais")
        BigDecimal valorUnitario
) {}
