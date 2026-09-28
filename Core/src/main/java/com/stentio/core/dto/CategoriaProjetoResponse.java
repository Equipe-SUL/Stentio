package com.stentio.core.dto;

import com.stentio.core.model.CategoriaProjeto;
import java.util.UUID;

public record CategoriaProjetoResponse(UUID id, String nome) {
    public CategoriaProjetoResponse(CategoriaProjeto categoriaProjeto) {
        this(
                categoriaProjeto.getId(),
                categoriaProjeto.getNome ()
        );


    }

}
