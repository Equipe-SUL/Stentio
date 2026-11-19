package com.stentio.core.config;

import java.util.UUID;

/**
 * O que o JWT diz sobre o usuário além do e-mail e do perfil, guardado em
 * {@code Authentication#getDetails()} pelo {@link JwtAuthFilter}.
 *
 * <p>Os dois campos podem ser nulos: tokens emitidos antes de o Auth incluir as claims
 * continuam válidos.
 */
public record DadosUsuarioToken(UUID usuarioId, String nome) {
}
