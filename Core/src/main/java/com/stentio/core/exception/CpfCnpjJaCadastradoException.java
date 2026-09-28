package com.stentio.core.exception;

public class CpfCnpjJaCadastradoException extends RuntimeException {

    public static final String MENSAGEM = "CPF/CNPJ já cadastrado para outro cliente";

    public CpfCnpjJaCadastradoException() {
        super(MENSAGEM);
    }

    public CpfCnpjJaCadastradoException(String mensagem) {
        super(mensagem);
    }
}
