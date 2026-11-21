package com.stentio.core.exception;

/**
 * US-024: a operação não cabe no estado atual da certificação.
 *
 * <p>Conflito, não erro de requisição: o pedido está correto e quem pediu tem permissão —
 * o que mudou foi o estado, possivelmente entre a tela e o clique.
 */
public class CertificacaoInvalidaException extends RuntimeException {
    public CertificacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
