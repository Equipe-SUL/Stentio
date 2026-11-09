package com.stentio.core.support;

import com.stentio.core.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Quem está fazendo a operação, no pouco que o Core sabe: o Auth emite um JWT com e-mail e
 * perfil, e nenhum id de usuário chega até aqui. Por isso o histórico identifica o autor
 * pelo e-mail — é o identificador estável que existe dos dois lados.
 *
 * <p>Montado no controller a partir do {@code Authentication} e passado adiante como
 * argumento, em vez de o service ler o SecurityContextHolder: assim a regra da RN.3 pode
 * ser testada sem levantar contexto de segurança.
 */
public record UsuarioAutenticado(String email, boolean administrador) {

    private static final String PREFIXO_ROLE = "ROLE_";

    public static UsuarioAutenticado de(Authentication autenticacao) {
        if (autenticacao == null) {
            return new UsuarioAutenticado("desconhecido", false);
        }
        boolean admin = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch((PREFIXO_ROLE + Role.ADMIN.name())::equals);
        return new UsuarioAutenticado(String.valueOf(autenticacao.getName()), admin);
    }
}
