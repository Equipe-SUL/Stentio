package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

// US-11: POST /api/v1/orcamentos. RN.5 (expiração no futuro) é validada no service.
public record OrcamentoCriacaoRequest(
        @JsonProperty("solicitacao_id")
        @NotNull(message = "Informe a solicitação")
        UUID solicitacaoId,

        @JsonProperty("cliente_id")
        @NotNull(message = "Informe o cliente")
        UUID clienteId,

        @JsonProperty("data_expiracao")
        @NotNull(message = "Informe a data de expiração")
        Instant dataExpiracao
) {
}
