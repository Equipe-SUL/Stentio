package com.stentio.emailservice.model;

// Espelho do enum Role do serviço Auth: os nomes precisam ser idênticos aos gravados na claim "role" do JWT.
public enum Role {
    ADMIN,
    ATENDENTE,
    GESTOR_PROJETO,
    FINANCEIRO
}
