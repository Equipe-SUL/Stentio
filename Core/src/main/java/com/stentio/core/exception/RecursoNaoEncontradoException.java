package com.stentio.core.exception;

import java.util.UUID;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(UUID id) {
        super("Recurso não encontrado: " + id);
    }
}