package com.stentio.emailservice.exception;

public class EmpresaNaoCadastradaException extends RuntimeException {

    public EmpresaNaoCadastradaException() {
        super("Configuração da empresa ainda não cadastrada");
    }
}
