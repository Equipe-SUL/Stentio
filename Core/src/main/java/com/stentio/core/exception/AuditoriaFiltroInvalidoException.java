package com.stentio.core.exception;

// US-019: parâmetro da consulta de auditoria fora do permitido; o handler global converte em HTTP 400.
public class AuditoriaFiltroInvalidoException extends RuntimeException {

    public AuditoriaFiltroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
