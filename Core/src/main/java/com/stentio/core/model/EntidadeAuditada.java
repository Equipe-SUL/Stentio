package com.stentio.core.model;

/**
 * US-019: as entidades cujas mudanças de status vão para o log de auditoria.
 *
 * <p>Os nomes são os do contrato da API ({@code ORDEM_SERVICO_ETAPA}, e não o nome da
 * classe), porque o front filtra por eles e o valor fica gravado em cada linha do log.
 */
public enum EntidadeAuditada {
    ORCAMENTO,
    ORDEM_SERVICO,
    ORDEM_SERVICO_ETAPA
}
