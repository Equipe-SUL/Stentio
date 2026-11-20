package com.stentio.core.model;

/**
 * US-019: por onde a mudança de status entrou no sistema.
 */
public enum OrigemAcao {
    /** Usuário da equipe, autenticado pelo JWT do Auth. */
    PAINEL_WEB,
    /** CA.4: ação externa sem login, autorizada por um link tokenizado (ex.: cliente aprovando o orçamento). */
    TOKEN_PUBLICO,
    /** Processo interno sem pessoa por trás (ex.: a OS criada pelo consumidor do RabbitMQ). */
    SISTEMA
}
