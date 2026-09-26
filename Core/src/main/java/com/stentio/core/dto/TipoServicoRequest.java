package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoServicoRequest(
        @NotBlank
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome
) {}