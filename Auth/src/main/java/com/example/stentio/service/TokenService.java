package com.example.stentio.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.stentio.model.Role;
import com.example.stentio.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    public static final long EXPIRES_IN = 432000L; // 5 dias em segundos

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("stentio-api")
                    .withSubject(usuario.getEmail())
                    .withClaim("role", usuario.getRole().name())
                    .withExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                    .sign(algoritmo);
        } catch (Exception exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public TokenDados validarToken(String token) {
        Algorithm algoritmo = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algoritmo)
                .withIssuer("stentio-api")
                .build();

        var decoded = verifier.verify(token);
        String email = decoded.getSubject();
        String role = decoded.getClaim("role").asString();

        return new TokenDados(email, Role.valueOf(role));
    }

    public record TokenDados(String email, Role role) {
    }
}