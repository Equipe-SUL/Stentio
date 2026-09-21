package com.example.stentio.core.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.stentio.core.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// O Core apenas valida tokens; quem os emite é o serviço Auth.
@Service
public class TokenService {

    private static final String ISSUER = "stentio-api";
    private static final String CLAIM_ROLE = "role";

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

        return new TokenDados(decodificado.getSubject(), Role.valueOf(role));
    }

    public record TokenDados(String email, Role role) {
    }
}
