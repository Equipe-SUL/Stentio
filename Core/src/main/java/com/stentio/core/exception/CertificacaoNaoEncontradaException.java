package com.stentio.core.exception;

import java.util.UUID;

public class CertificacaoNaoEncontradaException extends RuntimeException {
    public CertificacaoNaoEncontradaException(UUID id) {
        super("Certificação não encontrada: " + id);
    }
}
