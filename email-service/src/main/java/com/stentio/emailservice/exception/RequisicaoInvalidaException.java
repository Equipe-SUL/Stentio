package com.stentio.emailservice.exception;

// Falha de regra de negócio ou de entrada inválida: vira 400, não 500.
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
