package com.stentio.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * CA.2: chega como campos de um multipart/form-data, não como JSON.
 * O front manda cada campo com FormData.append("clienteId", ...) e os arquivos na parte "arquivos".
 * O status não entra aqui de propósito (CA.4): quem define é o backend.
 */
public record SolicitacaoRequest(

        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,

        @NotNull(message = "Categoria do projeto é obrigatória")
        UUID categoriaProjetoId,

        @NotNull(message = "Idioma de origem é obrigatório")
        UUID idiomaOrigemId,

        @NotNull(message = "Idioma de destino é obrigatório")
        UUID idiomaDestinoId,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String descricao
) {
}
