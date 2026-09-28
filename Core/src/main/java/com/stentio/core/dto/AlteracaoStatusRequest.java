package com.stentio.core.dto;

import jakarta.validation.constraints.NotNull;

// Corpo do PATCH /api/v1/recursos/{id}: { "ativo": false }
public record AlteracaoStatusRequest(

        @NotNull(message = "Informe o status ativo")
        Boolean ativo
) {
}
