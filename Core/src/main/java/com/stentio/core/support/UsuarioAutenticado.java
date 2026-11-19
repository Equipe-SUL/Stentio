package com.stentio.core.support;

import com.stentio.core.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

/**
 * Quem está fazendo a operação, no que o Core consegue saber pelo token.
 *
 * <p>O e-mail continua sendo a identificação usada no histórico: é o que existe dos dois
 * lados e o que uma pessoa reconhece ao ler um log. O {@code usuarioId} chegou com a
 * US-023, para o Core poder descobrir *qual recurso* está autenticado sem depender de um
 * dado que a própria pessoa pode trocar.
 *
 * <p>Montado no controller a partir do {@code Authentication} e passado adiante como
 * argumento, em vez de o service ler o SecurityContextHolder: assim as regras podem ser
 * testadas sem levantar contexto de segurança.
 */
public record UsuarioAutenticado(String email, UUID usuarioId, boolean administrador) {

    private static final String PREFIXO_ROLE = "ROLE_";

    /**
     * US-023 RN.3: token anterior a esta US não tem a claim userId, e quem foi autenticado
     * por {@code @WithMockUser} também não. Nos dois casos o id fica nulo e a sessão
     * continua valendo — quebrar aqui derrubaria a equipe inteira no dia do deploy.
     */
    public UsuarioAutenticado(String email, boolean administrador) {
        this(email, null, administrador);
    }

    public static UsuarioAutenticado de(Authentication autenticacao) {
        if (autenticacao == null) {
            return new UsuarioAutenticado("desconhecido", null, false);
        }
        boolean admin = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch((PREFIXO_ROLE + Role.ADMIN.name())::equals);

        return new UsuarioAutenticado(
                String.valueOf(autenticacao.getName()),
                usuarioIdDe(autenticacao),
                admin);
    }

    /** O JwtAuthFilter guarda o id em details; qualquer outro conteúdo ali é ignorado. */
    private static UUID usuarioIdDe(Authentication autenticacao) {
        return autenticacao.getDetails() instanceof UUID id ? id : null;
    }

    public boolean temUsuarioId() {
        return usuarioId != null;
    }
}
