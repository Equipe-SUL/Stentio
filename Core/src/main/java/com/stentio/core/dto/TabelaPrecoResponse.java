package com.stentio.core.dto;

import com.stentio.core.model.UnidadeCobranca;

import java.math.BigDecimal;
import java.util.UUID;

public record TabelaPrecoResponse(
        UUID id,
        TipoServicoResumo tipoServico,
        IdiomaResumo idiomaOrigem,
        IdiomaResumo idiomaDestino,
        UnidadeCobranca unidade,
        BigDecimal valorUnitario
) {
    public record TipoServicoResumo(UUID id, String nome) {}

    public record IdiomaResumo(UUID id, String nome, String codigoIso) {}
}
