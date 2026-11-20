package com.stentio.core.dto;

/**
 * US-013 CA.2: o convite, no formato que o email-service consome em {@code email.queue}.
 *
 * <p>Os nomes dos campos são os do {@code SendEmailRequest} do outro serviço, e não os do
 * Core, porque é ele quem lê esta mensagem. O contrato entre os dois é a fila.
 */
public record ConviteEtapaMensagem(String to, String subject, String body) {
}
