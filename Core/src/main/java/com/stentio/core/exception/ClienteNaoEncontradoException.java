package com.stentio.core.exception;

import java.util.UUID;

public class ClienteNaoEncontradoException extends RuntimeException {

    public ClienteNaoEncontradoException(UUID id) {
        super("Cliente não encontrado com ID: " + id);
    }

    public ClienteNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
