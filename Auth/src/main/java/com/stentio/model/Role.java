package com.stentio.model;

public enum Role {
    ADMIN,
    ATENDENTE,
    GESTOR_PROJETO,
    FINANCEIRO,
    /**
     * US-023: o profissional que executa as etapas. Diferente dos demais, não é equipe da
     * Aliança — cria a própria conta e enxerga só o que foi alocado a ele.
     */
    RECURSO
}
