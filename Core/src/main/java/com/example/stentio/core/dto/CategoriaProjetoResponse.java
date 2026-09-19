package com.example.stentio.core.dto;

import com.example.stentio.core.model.CategoriaProjeto;
import java.util.UUID;

public record CategoriaProjetoResponse(UUID id, String nome) {
    public CategoriaProjetoResponse(CategoriaProjeto categoriaProjeto) {
        this(
                categoriaProjeto.getId(),
                categoriaProjeto.getNome ()
        );


    }

}
