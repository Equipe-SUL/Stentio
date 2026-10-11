package com.stentio.core.exception;

public class LinkOrcamentoInvalidoException extends RuntimeException {
    public static final String MENSAGEM = "Link inválido, expirado ou já utilizado";

    public LinkOrcamentoInvalidoException() {
        super(MENSAGEM);
    }
}
