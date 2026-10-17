package com.stentio.core.exception;

// US-11: regra de negócio do orçamento violada pelos dados enviados (ex.: idiomas iguais, expiração no passado).
public class OrcamentoInvalidoException extends RuntimeException {
    public OrcamentoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
