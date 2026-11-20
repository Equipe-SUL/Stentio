package com.stentio.core.exception;

/**
 * US-013: a etapa não está no estado que a operação exige — disparar uma etapa já aceita,
 * entregar uma que ninguém assumiu, aceitar uma que outro já levou.
 *
 * <p>É 409 e não 400: o pedido está bem formado, quem mudou foi o mundo. A CA.3 pede
 * exatamente isso para o aceite concorrente.
 */
public class TransicaoEtapaInvalidaException extends RuntimeException {

    public TransicaoEtapaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
