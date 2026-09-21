package com.example.stentio.core.exception;

// Violação de regra de negócio do cadastro de recursos; o handler global converte em HTTP 400.
public class RecursoInvalidoException extends RuntimeException {

    public RecursoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
