package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmpresaPublicaResponse(
        String nome,
        String cnpj,
        String endereco,
        String telefone,
        @JsonProperty("logo_disponivel") Boolean logoDisponivel
) {
}
