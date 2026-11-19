package com.stentio.core.exception;

/**
 * US-023: o auto-cadastro do profissional não pode ser concluído — token sem a claim
 * userId (RN.3), e-mail diferente do da conta (RN.1) ou perfil já cadastrado.
 *
 * <p>É 400 e não 403: a pessoa está autenticada e tem o papel certo; o que está errado é
 * o pedido.
 */
public class AutoCadastroInvalidoException extends RuntimeException {

    public AutoCadastroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
