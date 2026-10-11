package com.stentio.core.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.StatusOrcamento;
import java.time.Instant;
import java.util.UUID;

// Contrato v1 proposto para os consumidores; não contém o token de acesso.
public record OrcamentoDecisaoEvento(
        @JsonProperty("evento_id") UUID eventoId,
        @JsonProperty("versao_schema") int versaoSchema,
        @JsonProperty("orcamento_id") UUID orcamentoId,
        StatusOrcamento status,
        String motivo,
        @JsonProperty("data_decisao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataDecisao) { }
