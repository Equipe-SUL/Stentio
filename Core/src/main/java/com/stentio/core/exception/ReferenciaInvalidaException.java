package com.stentio.core.exception;

import java.util.UUID;

// Id enviado no corpo que não existe no banco (cliente, categoria, idioma...).
// Diferente de "não encontrado": o recurso da URL existe, quem está errado é o dado enviado,
// por isso o handler devolve 400 e a mensagem diz qual entidade falhou.
public class ReferenciaInvalidaException extends RuntimeException {

    public ReferenciaInvalidaException(String entidade, UUID id) {
        super(entidade + " não encontrado(a): " + id);
    }
}
