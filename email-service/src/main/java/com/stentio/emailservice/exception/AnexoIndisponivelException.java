package com.stentio.emailservice.exception;

/**
 * US-015: o anexo obrigatório da entrega final não pôde ser montado.
 *
 * <p>Deixar a exceção subir faz o RabbitMQ devolver a mensagem para a fila, que é o
 * comportamento certo: o problema costuma ser o volume não estar montado ainda, e uma
 * nova tentativa resolve. Engolir o erro mandaria ao cliente um e-mail dizendo que o
 * trabalho está pronto, sem o trabalho.
 */
public class AnexoIndisponivelException extends RuntimeException {
    public AnexoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
