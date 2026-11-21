package com.stentio.core.exception;

/** US-015 CA.6 e RN.5: o arquivo não cabe no limite de anexo acordado com o email-service. */
public class AnexoAcimaDoLimiteException extends RuntimeException {
    public AnexoAcimaDoLimiteException(long tamanhoBytes, long limiteBytes) {
        super("O arquivo tem " + tamanhoBytes + " bytes e o limite de envio é " + limiteBytes);
    }
}
