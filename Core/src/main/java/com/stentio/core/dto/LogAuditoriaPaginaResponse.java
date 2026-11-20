package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.LogAuditoria;
import com.stentio.core.model.OrigemAcao;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-019: resposta de GET /api/v1/auditoria/logs, no formato do contrato da US.
 *
 * <p>{@code tamanho_pagina} e {@code total_paginas} não estão no contrato, mas são o que o
 * front precisa para desenhar a paginação sem refazer a conta. Campos a mais não quebram
 * quem lê só os do contrato.
 */
public record LogAuditoriaPaginaResponse(
        @JsonProperty("total_registros") long totalRegistros,
        @JsonProperty("pagina_atual") int paginaAtual,
        @JsonProperty("tamanho_pagina") int tamanhoPagina,
        @JsonProperty("total_paginas") int totalPaginas,
        List<Log> logs
) {

    /** "pagina_atual" começa em 1, como nas demais listagens do Core. */
    public static LogAuditoriaPaginaResponse de(Page<LogAuditoria> pagina) {
        return new LogAuditoriaPaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalPages(),
                pagina.getContent().stream().map(Log::de).toList());
    }

    public record Log(
            UUID id,
            EntidadeAuditada entidade,
            @JsonProperty("entidade_id") UUID entidadeId,
            @JsonProperty("referencia_legivel") String referenciaLegivel,
            @JsonProperty("status_anterior") String statusAnterior,
            @JsonProperty("status_novo") String statusNovo,
            Usuario usuario,
            @JsonProperty("origem_acao") OrigemAcao origemAcao,
            @JsonProperty("data_hora") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataHora,
            String observacao
    ) {
        static Log de(LogAuditoria log) {
            return new Log(
                    log.getId(),
                    log.getEntidade(),
                    log.getEntidadeId(),
                    log.getReferenciaLegivel(),
                    log.getStatusAnterior(),
                    log.getStatusNovo(),
                    new Usuario(log.getUsuarioId(), log.getUsuarioNome(), log.getUsuarioPerfil()),
                    log.getOrigemAcao(),
                    log.getDataHora(),
                    log.getObservacao());
        }
    }

    public record Usuario(UUID id, String nome, String perfil) {
    }
}
