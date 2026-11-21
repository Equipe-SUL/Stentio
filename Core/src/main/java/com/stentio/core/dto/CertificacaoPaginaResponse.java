package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.RecursoCertificacao;
import org.springframework.data.domain.Page;

import java.util.List;

/** US-024 CA.2: a fila de análise, no mesmo envelope dos outros módulos paginados. */
public record CertificacaoPaginaResponse(
        @JsonProperty("total_registros") long totalRegistros,
        @JsonProperty("pagina_atual") int paginaAtual,
        @JsonProperty("tamanho_pagina") int tamanhoPagina,
        @JsonProperty("total_paginas") int totalPaginas,
        List<CertificacaoResponse> certificacoes
) {
    public static CertificacaoPaginaResponse de(Page<RecursoCertificacao> pagina) {
        return new CertificacaoPaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalPages(),
                pagina.getContent().stream().map(CertificacaoResponse::de).toList());
    }
}
