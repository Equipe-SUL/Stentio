package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.StatusOrcamento;

import java.time.Instant;
import java.util.UUID;

public record OrcamentoAprovacaoResponse(
        UUID id,
        StatusOrcamento status,
        @JsonProperty("token_aprovacao") UUID tokenAprovacao,
        @JsonProperty("data_expiracao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataExpiracao,
        String mensagem) {
}
