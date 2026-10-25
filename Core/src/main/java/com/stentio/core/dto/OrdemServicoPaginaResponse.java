package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

// Mesmo envelope do OrcamentoPaginaResponse: "pagina" começa em 1 para o front.
public record OrdemServicoPaginaResponse(
        int pagina,
        @JsonProperty("tamanho_pagina") int tamanhoPagina,
        @JsonProperty("total_elementos") long totalElementos,
        List<OrdemServicoResumoResponse> conteudo
) {

    public static OrdemServicoPaginaResponse de(Page<OrdemServicoResumoResponse> pagina) {
        return new OrdemServicoPaginaResponse(
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getContent());
    }
}
