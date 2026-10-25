package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EmpresaPublicaResponse(
        String nome,
        String cnpj,
        String endereco,
        String telefone,
        @JsonProperty("logo_disponivel")
        @JsonAlias({"logoDisponivel", "logo_disponivel"})
        Boolean logoDisponivel
) {
}
