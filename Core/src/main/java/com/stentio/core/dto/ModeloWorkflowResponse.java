package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

// US-011: modelo com as etapas que serão clonadas na OS.
public record ModeloWorkflowResponse(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        List<Etapa> etapas
) {

    public record Etapa(
            UUID id,
            Integer ordem,
            String nome,
            @JsonProperty("tipo_servico_id") UUID tipoServicoId,
            @JsonProperty("tipo_servico_nome") String tipoServicoNome,
            @JsonProperty("prazo_estimado_horas") Integer prazoEstimadoHoras
    ) {
    }
}
