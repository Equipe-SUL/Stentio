package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.StatusOrcamento;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// US-11: GET /api/v1/orcamentos. "pagina" começa em 1 para o front.
public record OrcamentoPaginaResponse(
        int pagina,
        @JsonProperty("tamanho_pagina") int tamanhoPagina,
        @JsonProperty("total_elementos") long totalElementos,
        List<Resumo> conteudo
) {

    public static OrcamentoPaginaResponse de(Page<Orcamento> pagina) {
        return new OrcamentoPaginaResponse(
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getContent().stream().map(Resumo::de).toList());
    }

    // Sem itens: a listagem não toca nas coleções, evitando N+1.
    public record Resumo(
            UUID id,
            int versao,
            @JsonProperty("cliente_id") UUID clienteId,
            @JsonProperty("solicitacao_id") UUID solicitacaoId,
            StatusOrcamento status,
            @JsonProperty("valor_total") BigDecimal valorTotal,
            @JsonProperty("data_expiracao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataExpiracao
    ) {
        // getId() de um proxy LAZY não dispara consulta: cliente e solicitação não são carregados.
        static Resumo de(Orcamento orcamento) {
            return new Resumo(
                    orcamento.getId(),
                    orcamento.getNumeroVersao(),
                    orcamento.getCliente().getId(),
                    orcamento.getSolicitacao() == null ? null : orcamento.getSolicitacao().getId(),
                    orcamento.getStatus(),
                    orcamento.getValorTotal(),
                    orcamento.getDataExpiracao());
        }
    }
}
