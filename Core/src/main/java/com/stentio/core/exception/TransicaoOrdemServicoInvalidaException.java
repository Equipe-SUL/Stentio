package com.stentio.core.exception;

/**
 * US-015: a ordem de serviço não está no estado que a operação exige.
 *
 * <p>Conflito, não erro de requisição: o pedido está bem formado e quem pediu tem
 * permissão — o que não bate é o estado atual, e ele pode ter mudado entre a tela do
 * gestor e o clique.
 */
public class TransicaoOrdemServicoInvalidaException extends RuntimeException {
    public TransicaoOrdemServicoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
