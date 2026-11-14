package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.SituacaoEtapa;

import java.time.Instant;
import java.util.UUID;

// US-012: resposta do POST de alocação, exatamente no formato do contrato do card.
public record AlocacaoEtapaResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        @JsonProperty("status_etapa") SituacaoEtapa statusEtapa,
        Recurso recurso,
        @JsonProperty("data_limite_entrega") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimiteEntrega
) {

    public record Recurso(UUID id, String nome) {
    }

    public static AlocacaoEtapaResponse de(EtapaOrdemServico etapa) {
        return new AlocacaoEtapaResponse(
                etapa.getId(),
                etapa.situacao(),
                new Recurso(etapa.getRecurso().getId(), etapa.getRecurso().getNome()),
                etapa.getDataLimiteEntrega());
    }
}
