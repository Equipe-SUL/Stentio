package com.stentio.core.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.stentio.core.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

// O Core apenas valida tokens; quem os emite é o serviço Auth.
@Service
public class TokenService {

    private static final String ISSUER = "stentio-api";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "userId";
    // US-019: snapshot do nome para o log de auditoria.
    private static final String CLAIM_NOME = "nome";

    private final JWTVerifier verifier;

    public TokenService(@Value("${api.security.token.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(ISSUER)
                .build();
    }

    public TokenDados validarToken(String token) {
        DecodedJWT decodificado = verifier.verify(token);
        String role = decodificado.getClaim(CLAIM_ROLE).asString();

        if (role == null) {
            throw new JWTVerificationException("Token sem a claim role");
        }

        return new TokenDados(
                decodificado.getSubject(),
                Role.valueOf(role),
                lerUsuarioId(decodificado.getClaim(CLAIM_USER_ID).asString()),
                decodificado.getClaim(CLAIM_NOME).asString());
    }

    /**
     * Token emitido antes de o Auth incluir as claims userId e nome continua válido: o
     * usuário segue autenticado, só sem esses dados. Derrubar a sessão de todo mundo no dia
     * do deploy seria pior do que um log que mostra o e-mail no lugar do nome.
     */
    private static UUID lerUsuarioId(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** {@code usuarioId} e {@code nome} podem vir nulos em tokens antigos. */
    public record TokenDados(String email, Role role, UUID usuarioId, String nome) {
    }
}
