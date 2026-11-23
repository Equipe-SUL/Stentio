package com.stentio.core.model;

public enum StatusOrdemServico {
    /** Nasce assim ao aprovar o orçamento (US-16): existe, mas ninguém planejou a produção. */
    PENDENTE_ALOCACAO,
    /** US-011: o gestor já definiu a sequência de etapas; falta alocar os recursos (US-012). */
    EM_PLANEJAMENTO,
    /** US-014: pelo menos uma etapa da OS foi entregue e a produção está em andamento. */
    EM_EXECUCAO,
    /** US-014: todas as etapas foram entregues; a OS aguarda verificação final (US-015). */
    AGUARDANDO_VERIFICACAO,
    /** US-015: o gestor aprovou, o e-mail ao cliente foi publicado e a OS está fechada. */
    ENCERRADA
}
