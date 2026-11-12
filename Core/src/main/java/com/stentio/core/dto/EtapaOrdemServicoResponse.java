package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.SituacaoEtapa;

import java.time.Instant;
import java.util.UUID;

/**
 * US-011: uma etapa na resposta. O tipo de serviço vai como id mais nome, no mesmo formato
 * que a listagem de OS usa para o cliente — a tela mostra o nome e guarda o id para editar.
 *
 * <p>US-012: {@code status} passou a ser a situação (ver {@link SituacaoEtapa}), que mostra
 * {@code ALOCADO} quando há recurso. Para quem consome, nada mudou enquanto a etapa não é
 * alocada — e é nesse estado que toda etapa da US-011 vivia.
 */
public record EtapaOrdemServicoResponse(
        UUID id,
        Integer ordem,
        String nome,
        @JsonProperty("tipo_servico_id") UUID tipoServicoId,
        @JsonProperty("tipo_servico_nome") String tipoServicoNome,
        SituacaoEtapa status,
        @JsonProperty("prazo_estimado_horas") Integer prazoEstimadoHoras,
        @JsonProperty("recurso_id") UUID recursoId,
        @JsonProperty("recurso_nome") String recursoNome,
        @JsonProperty("data_limite_entrega") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimiteEntrega
) {
}
