package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.util.UUID;

/**
 * US-011: uma etapa na resposta. O tipo de serviço vai como id mais nome, no mesmo formato
 * que a listagem de OS usa para o cliente — a tela mostra o nome e guarda o id para editar.
 */
public record EtapaOrdemServicoResponse(
        UUID id,
        Integer ordem,
        String nome,
        @JsonProperty("tipo_servico_id") UUID tipoServicoId,
        @JsonProperty("tipo_servico_nome") String tipoServicoNome,
        StatusEtapaOrdemServico status,
        @JsonProperty("prazo_estimado_horas") Integer prazoEstimadoHoras,
        // US-012 preenche ao alocar. Enquanto for nulo, a sequência ainda pode ser remontada.
        @JsonProperty("recurso_id") UUID recursoId
) {
}
