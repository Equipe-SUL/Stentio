package com.stentio.core.dto;

import com.stentio.core.model.StatusOrcamento;

import java.time.Instant;
import java.util.UUID;

public record DashboardOrcamentoPendenciaProjection(
        UUID id,
        StatusOrcamento status,
        Instant dataExpiracao,
        Instant criadoEm,
        String nomeEmpresa
) {
}
