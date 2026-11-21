package com.stentio.core.dto;

import jakarta.validation.constraints.Size;

/** US-015: recado opcional do gestor no corpo do e-mail ao cliente. */
public record AprovarEntregaFinalRequest(
        @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres")
        String mensagem
) {
}
