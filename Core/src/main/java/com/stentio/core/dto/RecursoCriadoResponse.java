package com.stentio.core.dto;

// Resposta do POST: a mensagem do Cenário 1 e o recurso criado, com o id para o front navegar.
public record RecursoCriadoResponse(String mensagem, RecursoResponse recurso) {

    private static final String MENSAGEM_SUCESSO = "Recurso cadastrado com sucesso";

    public static RecursoCriadoResponse de(RecursoResponse recurso) {
        return new RecursoCriadoResponse(MENSAGEM_SUCESSO, recurso);
    }
}
