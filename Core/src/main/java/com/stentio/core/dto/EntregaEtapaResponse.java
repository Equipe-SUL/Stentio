package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;

import java.time.Instant;
import java.util.UUID;

/** US-013 e US-014: o recibo da entrega e o resultado do avanço do workflow. */
public record EntregaEtapaResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        StatusEtapaOrdemServico status,
        @JsonProperty("entregue_em") Instant entregueEm,
        @JsonProperty("arquivo_nome") String arquivoNome,
        @JsonProperty("arquivo_url") String arquivoUrl,
        Avanco avanco,
        @JsonProperty("ordem_servico") OrdemServico ordemServico
) {
    public record Avanco(
            @JsonProperty("proxima_etapa_id") UUID proximaEtapaId,
            @JsonProperty("proxima_etapa_status") StatusEtapaOrdemServico proximaEtapaStatus,
            @JsonProperty("convite_automatico") boolean conviteAutomatico,
            @JsonProperty("recursos_notificados") int recursosNotificados
    ) {
    }

    public record OrdemServico(UUID id, StatusOrdemServico status) {
    }
}
