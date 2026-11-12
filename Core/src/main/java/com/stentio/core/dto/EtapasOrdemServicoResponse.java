package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusOrdemServico;

import java.util.List;
import java.util.UUID;

// US-011: a árvore montada, no formato do contrato do card.
public record EtapasOrdemServicoResponse(
        @JsonProperty("ordem_servico_id") UUID ordemServicoId,
        @JsonProperty("status_os") StatusOrdemServico statusOs,
        List<EtapaOrdemServicoResponse> etapas
) {
}
