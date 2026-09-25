package com.stentio.core.exception;

// RN.1: mesma combinação de tipo de serviço + idiomas + unidade; o handler global converte em HTTP 409.
public class TabelaPrecoDuplicadaException extends RuntimeException {

    public static final String MENSAGEM = "Já existe um preço para essa combinação de serviço, idiomas e unidade";

    public TabelaPrecoDuplicadaException() {
        super(MENSAGEM);
    }
}
