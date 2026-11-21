package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-015 CA.10: o motivo é obrigatório.
 *
 * <p>Ele não é burocracia: vai no e-mail ao profissional e fica no histórico da etapa. Uma
 * reprovação sem motivo devolve trabalho sem dizer o que corrigir.
 */
public record ReprovarEntregaFinalRequest(
        @NotBlank(message = "Informe o motivo da reprovação")
        @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres")
        String motivo
) {
}
