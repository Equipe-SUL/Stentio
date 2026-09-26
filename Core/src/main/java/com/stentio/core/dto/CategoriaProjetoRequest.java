package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaProjetoRequest (

    @NotBlank
    @Size(max = 40, message = "Nome deve ter no máximo 40 caracteres")
    String nome
){}
