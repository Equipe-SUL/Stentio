package com.stentio.core.model;

public enum StatusOrdemServico {
    /** Nasce assim ao aprovar o orçamento (US-16): existe, mas ninguém planejou a produção. */
    PENDENTE_ALOCACAO,
    /** US-011: o gestor já definiu a sequência de etapas; falta alocar os recursos (US-012). */
    EM_PLANEJAMENTO
}
