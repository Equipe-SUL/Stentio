package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IdiomaRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "Código ISO é obrigatório")
        @Size(max = 10, message = "Código ISO deve ter no máximo 10 caracteres")
        @Pattern(regexp = "^[A-Za-z]{2,3}(-[A-Za-z]{2,4})?$",
                message = "Código ISO inválido (ex: pt, en, pt-BR)")
        String codigoIso
) { }