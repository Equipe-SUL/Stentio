package com.example.stentio.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail: " + email);
    }
}