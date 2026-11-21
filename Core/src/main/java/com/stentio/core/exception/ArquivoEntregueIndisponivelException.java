package com.stentio.core.exception;

/**
 * US-015 CA.6: o banco aponta para um arquivo que o armazenamento não devolve.
 *
 * <p>422 e não 404: a ordem de serviço existe e a requisição está correta — o que não dá
 * para processar é o conteúdo que ela referencia.
 */
public class ArquivoEntregueIndisponivelException extends RuntimeException {
    public ArquivoEntregueIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
