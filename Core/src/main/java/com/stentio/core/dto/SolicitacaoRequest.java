package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitacaoRequest(
        @NotNull(message = "Tipo de serviço é obrigatório")
        UUID tipoServicoId,

        @NotNull(message = "Idioma que irá ser traduzido é obrigatório")
        UUID idiomaOrigemId,

        @NotNull(message = "Idioma final é obrigatório")
        UUID idiomaDestinoId,

        @NotBlank(message = "Descrição da tradução é obrigatória")
        String descricao
) {
}