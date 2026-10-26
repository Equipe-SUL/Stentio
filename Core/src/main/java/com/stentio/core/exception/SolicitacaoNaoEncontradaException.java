package com.stentio.core.exception;

import java.util.UUID;

public class SolicitacaoNaoEncontradaException extends RuntimeException {

    public SolicitacaoNaoEncontradaException(UUID id) {
        super("Solicitação não encontrada: " + id);
    }
}
