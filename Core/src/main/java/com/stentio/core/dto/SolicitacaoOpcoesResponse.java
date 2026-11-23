package com.stentio.core.dto;

import java.util.List;
import java.util.UUID;

public record SolicitacaoOpcoesResponse(
        List<CategoriaOpcao> categoriasProjeto,
        List<IdiomaOpcao> idiomas
) {

    public SolicitacaoOpcoesResponse {
        categoriasProjeto = List.copyOf(categoriasProjeto);
        idiomas = List.copyOf(idiomas);
    }

    public record CategoriaOpcao(UUID id, String nome) {
    }

    public record IdiomaOpcao(UUID id, String nome, String codigoIso) {
    }
}
