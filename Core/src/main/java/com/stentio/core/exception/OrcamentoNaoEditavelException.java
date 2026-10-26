package com.stentio.core.exception;

// US-11: itens só podem ser alterados enquanto o orçamento está em elaboração (decisão registrada na US).
public class OrcamentoNaoEditavelException extends RuntimeException {
    public OrcamentoNaoEditavelException() {
        super("Os itens só podem ser alterados enquanto o orçamento estiver em elaboração");
    }
}
