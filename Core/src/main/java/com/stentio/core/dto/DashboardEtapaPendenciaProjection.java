package com.stentio.core.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardEtapaPendenciaProjection(
        UUID id,
        String nome,
        Instant dataLimiteEntrega,
        UUID ordemServicoId,
        Instant ordemServicoCriadoEm
) {
}
