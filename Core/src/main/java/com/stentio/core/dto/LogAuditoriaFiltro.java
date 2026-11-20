package com.stentio.core.dto;

import com.stentio.core.model.EntidadeAuditada;

import java.time.Instant;
import java.util.UUID;

/**
 * US-019 CA.3: filtros da consulta de auditoria. Todos opcionais e combináveis.
 *
 * @param busca trecho do código/referência (ex.: "ORC-2026-A1B2") ou do id da entidade
 */
public record LogAuditoriaFiltro(
        EntidadeAuditada entidade,
        UUID entidadeId,
        UUID usuarioId,
        Instant dataInicio,
        Instant dataFim,
        String busca
) {
}
