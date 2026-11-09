package com.stentio.core.exception;

/**
 * US-011 RN.3: a sequência já tem etapa alocada a um recurso, então remontá-la quebraria o
 * trabalho de quem já foi acionado. O handler global converte em HTTP 409.
 */
public class EtapasBloqueadasException extends RuntimeException {

    public EtapasBloqueadasException(String mensagem) {
        super(mensagem);
    }
}
