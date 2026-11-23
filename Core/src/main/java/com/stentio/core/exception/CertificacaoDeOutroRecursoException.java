package com.stentio.core.exception;

/**
 * US-024 RN.8: o documento pedido é de outro profissional.
 *
 * <p>403 e não 404: esconder a existência não ajuda ninguém aqui — o id veio de algum
 * lugar, e dizer "não é sua" é mais honesto do que fingir que não existe.
 */
public class CertificacaoDeOutroRecursoException extends RuntimeException {
    public CertificacaoDeOutroRecursoException(String mensagem) {
        super(mensagem);
    }
}
