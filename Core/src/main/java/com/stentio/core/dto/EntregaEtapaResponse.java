package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.time.Instant;
import java.util.UUID;

/** US-013 CA.5: o recibo da entrega. */
public record EntregaEtapaResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        StatusEtapaOrdemServico status,
        @JsonProperty("entregue_em") Instant entregueEm,
        @JsonProperty("arquivo_nome") String arquivoNome
) {
}
