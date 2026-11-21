package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;

import java.util.UUID;

/** US-015 CA.11: onde a etapa e a OS ficaram depois da reprovação. */
public record ReprovacaoEntregaFinalResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        @JsonProperty("etapa_status") StatusEtapaOrdemServico etapaStatus,
        @JsonProperty("ordem_servico_status") StatusOrdemServico ordemServicoStatus
) {
}
