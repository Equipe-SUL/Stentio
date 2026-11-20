package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.util.UUID;

/** O estado da etapa depois de uma transição que não tem mais nada a dizer. */
public record EtapaStatusResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        StatusEtapaOrdemServico status
) {
}
