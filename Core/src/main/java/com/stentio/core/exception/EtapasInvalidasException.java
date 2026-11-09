package com.stentio.core.exception;

// US-011: sequência de etapas que viola RN.1 ou RN.2; o handler global converte em HTTP 400.
public class EtapasInvalidasException extends RuntimeException {

    public EtapasInvalidasException(String mensagem) {
        super(mensagem);
    }
}
