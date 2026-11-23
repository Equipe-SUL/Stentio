package com.stentio.core.dto;

import com.stentio.core.model.StatusSolicitacao;

import java.util.UUID;

public record SolicitacaoPublicaResponse(UUID id, StatusSolicitacao status) {
}
