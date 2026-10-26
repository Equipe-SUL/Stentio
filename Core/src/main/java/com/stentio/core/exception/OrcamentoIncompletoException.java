package com.stentio.core.exception;

public class OrcamentoIncompletoException extends RuntimeException {
    public static final String MENSAGEM =
            "O orçamento precisa estar completo, associado a um cliente e possuir itens com valores calculados";

    public OrcamentoIncompletoException() {
        super(MENSAGEM);
    }
}
