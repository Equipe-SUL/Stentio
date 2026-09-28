package com.example.stentio.exception;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado com o id: " + id);
    }

    public UsuarioNaoEncontradoException(String email) {
        super("Usuário não encontrado com o email: " + email);
    }
}