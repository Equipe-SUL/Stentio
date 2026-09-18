package com.example.stentio.core.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public static final String MENSAGEM = "Já existe um recurso com esse e-mail";

    public EmailJaCadastradoException() {
        super(MENSAGEM);
    }
}
