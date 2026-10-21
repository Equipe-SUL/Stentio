package com.stentio.exception;

public class CredenciaisLoginException extends RuntimeException{
    public CredenciaisLoginException() {
        super("Email ou senha inválidos");
    }
}
