package com.stentio.core.exception;

/**
 * US-024 RN.3: o profissional tentou certificar um par que não está no cadastro dele.
 *
 * <p>422 e não 400: a requisição está bem formada e os idiomas existem — o que não faz
 * sentido é certificar um par que ele não declarou atender.
 */
public class ParNaoAtendidoException extends RuntimeException {
    public ParNaoAtendidoException(String mensagem) {
        super(mensagem);
    }
}
