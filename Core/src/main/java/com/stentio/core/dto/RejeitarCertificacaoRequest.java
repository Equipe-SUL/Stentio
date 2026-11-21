package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-024 CA.2: o motivo é obrigatório.
 *
 * <p>Ele vai no e-mail ao profissional e fica visível no portal. Rejeitar sem motivo
 * devolveria o documento sem dizer o que corrigir, e o reenvio seria às cegas.
 */
public record RejeitarCertificacaoRequest(
        @NotBlank(message = "Informe o motivo da rejeição")
        @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres")
        String motivo
) {
}
