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
                lerUsuarioId(decodificado.getClaim(CLAIM_USER_ID).asString()));
    }

    /**
     * US-023 RN.3: a claim userId só existe em token emitido a partir desta US. Ausente,
     * o usuário segue autenticado sem id — é o que mantém de pé a sessão de quem já
     * estava logado quando o deploy aconteceu. Quem precisa do id (o portal do recurso)
     * é que recusa a requisição, e com mensagem própria.
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

    public record TokenDados(String email, Role role, UUID usuarioId) {
    }
}
