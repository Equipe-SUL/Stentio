package com.stentio.core.exception;

// Parâmetro de consulta fora do permitido; o handler global converte em HTTP 400.
public class OrdemServicoInvalidaException extends RuntimeException {

    public OrdemServicoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
