package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardResumoResponse(
        @JsonProperty("total_orcamentos_em_elaboracao") long totalOrcamentosEmElaboracao,
        @JsonProperty("total_orcamentos_aguardando_aprovacao") long totalOrcamentosAguardandoAprovacao,
        @JsonProperty("total_os_em_andamento") long totalOsEmAndamento,
        @JsonProperty("total_etapas_atrasadas") long totalEtapasAtrasadas,
        @JsonProperty("taxa_conversao_orcamentos") double taxaConversaoOrcamentos
) {
}
