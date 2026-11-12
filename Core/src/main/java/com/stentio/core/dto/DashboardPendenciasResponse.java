package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardPendenciasResponse(
        @JsonProperty("total_itens") long totalItens,
        List<Pendencia> pendencias
) {

    public record Pendencia(
            UUID id,
            @JsonProperty("tipo_entidade") TipoEntidade tipoEntidade,
            String titulo,
            String status,
            @JsonProperty("data_limite") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimite,
            @JsonProperty("link_acao") String linkAcao
    ) {
    }

    public enum TipoEntidade {
        ETAPA_OS,
        ORCAMENTO
    }
}
