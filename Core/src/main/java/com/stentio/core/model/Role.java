package com.stentio.core.model;

// Espelho do enum Role do serviço Auth: os nomes precisam ser idênticos aos gravados na claim "role" do JWT.
public enum Role {
    ADMIN,
    ATENDENTE,
    GESTOR_PROJETO,
    FINANCEIRO,
    /**
     * US-023: o profissional que executa as etapas.
     *
     * <p>Precisa existir aqui <b>antes</b> de o Auth começar a emitir tokens com ele: o
     * TokenService faz {@code Role.valueOf} na claim, e um papel desconhecido derruba a
     * autenticação. Na hora de publicar, Core primeiro.
     */
    RECURSO
}
