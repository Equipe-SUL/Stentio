package com.stentio.core.exception;

/**
 * US-012: a alocação pedida não é aceitável — recurso incompatível com o par de idiomas
 * (RN.1) ou com o tipo de serviço da etapa (RN.2), recurso inativo, ou data limite no
 * passado. É erro de entrada, então vira 400.
 */
public class AlocacaoInvalidaException extends RuntimeException {

    public AlocacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
