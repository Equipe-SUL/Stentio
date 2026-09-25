package com.stentio.emailservice.exception;

// A empresa existe, mas ainda não recebeu logo: 404, não 400.
public class LogoNaoEncontradaException extends RuntimeException {

    public LogoNaoEncontradaException() {
        super("Nenhuma logo enviada para a empresa");
    }
}
