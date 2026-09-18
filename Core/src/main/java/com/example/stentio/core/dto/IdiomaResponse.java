package com.example.stentio.core.dto;

import com.example.stentio.core.model.Idioma;

import java.util.UUID;

public record IdiomaResponse(UUID id, String nome, String codigoIso, boolean ativo) {

    public IdiomaResponse(Idioma idioma) {
        this(
                idioma.getId(),
                idioma.getNome(),
                idioma.getCodigoIso(),
                idioma.getAtivo()
        );
    }
}