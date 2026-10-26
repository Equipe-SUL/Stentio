package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.StatusOrcamento;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoDecisaoResponse(
        UUID id,
        StatusOrcamento status,
        @JsonProperty("data_decisao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataDecisao) { }
