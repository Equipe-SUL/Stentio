package com.stentio.core.exception;

public class OrcamentoStatusInvalidoException extends RuntimeException {
    public OrcamentoStatusInvalidoException() {
        super("Somente orçamentos em elaboração, aguardando aprovação ou rejeitados podem ser enviados");
    }
}
