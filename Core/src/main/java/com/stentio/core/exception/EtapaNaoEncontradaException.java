package com.stentio.core.exception;

import java.util.UUID;

/**
 * US-012: a etapa não existe, ou não pertence à ordem de serviço informada. Os dois casos
 * respondem a mesma coisa de propósito: dizer "existe, mas é de outra OS" contaria a quem
 * não deveria saber que aquele id é válido.
 */
public class EtapaNaoEncontradaException extends RuntimeException {

    public EtapaNaoEncontradaException(UUID id) {
        super("Etapa não encontrada na ordem de serviço informada: " + id);
    }
}
