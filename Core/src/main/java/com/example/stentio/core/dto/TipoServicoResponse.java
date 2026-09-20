package com.example.stentio.core.dto;

import com.example.stentio.core.model.TipoServico;
import java.util.UUID;

public record TipoServicoResponse(
        UUID id,
        String nome,
        boolean ativo
) {
    public TipoServicoResponse(TipoServico tipoServico) {
        this(tipoServico.getId(), tipoServico.getNome(), tipoServico.isAtivo());
    }
}