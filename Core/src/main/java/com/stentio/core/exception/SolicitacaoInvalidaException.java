package com.stentio.core.exception;

// Violação de regra de negócio da solicitação; o handler global converte em HTTP 400.
public class SolicitacaoInvalidaException extends RuntimeException {

    public SolicitacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
