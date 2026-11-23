package com.stentio.core.exception;

import java.util.UUID;

// US-011: modelo de workflow inexistente ou inativo; o handler global converte em HTTP 404.
public class ModeloWorkflowNaoEncontradoException extends RuntimeException {

    public ModeloWorkflowNaoEncontradoException(UUID id) {
        super("Modelo de workflow não encontrado: " + id);
    }

    public ModeloWorkflowNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
