package com.stentio.core.exception;

/**
 * US-012 RN.3: a etapa já começou e trocar o recurso passou a exigir permissão
 * administrativa.
 *
 * <p>É 403 e não 409 porque a operação não está em conflito com o estado para todo mundo —
 * um ADMIN pode fazer exatamente isto. O que falta é perfil, e esse é o significado do 403.
 */
public class AlocacaoNaoPermitidaException extends RuntimeException {

    public AlocacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
