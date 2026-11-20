package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.time.Instant;
import java.util.UUID;

/** US-013 CA.3: a confirmação de que a etapa é sua. */
public record AceiteEtapaResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        StatusEtapaOrdemServico status,
        @JsonProperty("aceito_em") Instant aceitoEm,
        @JsonProperty("data_limite_entrega") Instant dataLimiteEntrega
) {
}
