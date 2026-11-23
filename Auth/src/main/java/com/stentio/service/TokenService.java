package com.stentio.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.stentio.model.Role;
import com.stentio.model.Usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TokenService {

    public static final long EXPIRES_IN = 432000L; // 5 dias em segundos

    private static final String CLAIM_ROLE = "role";
    /**
     * US-023 CA.4: o Core precisa saber *qual* usuário está autenticado, não só o papel.
     * O e-mail já ia no subject, mas usá-lo como chave estrangeira amarraria o vínculo do
     * recurso a um dado que o próprio usuário pode trocar.
     */
    private static final String CLAIM_USER_ID = "userId";
    /**
     * US-019: o log de auditoria guarda o nome de quem alterou o status como snapshot. O
     * usuário vive no Auth, então o Core só conhece o que vem no token.
     */
    private static final String CLAIM_NOME = "nome";

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("stentio-api")
                    .withSubject(usuario.getEmail())
                    .withClaim(CLAIM_ROLE, usuario.getRole().name())
                    .withClaim(CLAIM_USER_ID, usuario.getId().toString())
                    .withClaim(CLAIM_NOME, usuario.getNome())
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
        String role = decoded.getClaim(CLAIM_ROLE).asString();

        return new TokenDados(email, Role.valueOf(role), lerUsuarioId(decoded.getClaim(CLAIM_USER_ID).asString()));
    }

    /**
     * RN.3: token emitido antes desta US não tem a claim. Voltar nulo mantém a sessão de
     * quem já estava logado, em vez de derrubar todo mundo no dia do deploy.
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
