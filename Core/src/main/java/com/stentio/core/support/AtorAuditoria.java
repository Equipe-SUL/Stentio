package com.stentio.core.support;

import com.stentio.core.config.DadosUsuarioToken;
import com.stentio.core.model.OrigemAcao;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;
import java.util.UUID;

/**
 * US-019: quem fez a mudança de status, como vai ficar gravado no log.
 *
 * <p>Para a equipe, é um snapshot do JWT ({@link #deAutenticacao}). Para quem age sem login
 * (CA.4), o service que trata o link informa o ator explicitamente — {@link #clienteLinkPublico()}.
 * Sem login e sem ator informado, quem agiu foi o próprio sistema.
 */
public record AtorAuditoria(UUID usuarioId, String nome, String perfil, OrigemAcao origem) {

    public static final String PERFIL_CLIENTE_EXTERNO = "CLIENTE_EXTERNO";
    public static final String PERFIL_SISTEMA = "SISTEMA";

    private static final String PREFIXO_ROLE = "ROLE_";

    public AtorAuditoria {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(perfil, "perfil");
        Objects.requireNonNull(origem, "origem");
    }

    /** CA.4: o cliente aprovando ou rejeitando o orçamento pelo link do e-mail. */
    public static AtorAuditoria clienteLinkPublico() {
        return new AtorAuditoria(null, "Cliente (Link Público)", PERFIL_CLIENTE_EXTERNO, OrigemAcao.TOKEN_PUBLICO);
    }

    /** Processos internos: consumidores de fila, agendamentos. */
    public static AtorAuditoria sistema() {
        return new AtorAuditoria(null, "Sistema", PERFIL_SISTEMA, OrigemAcao.SISTEMA);
    }

    /**
     * Snapshot do usuário autenticado. O nome vem da claim do JWT; token antigo, sem a claim,
     * cai para o e-mail, que ainda identifica a pessoa para quem lê o log.
     */
    public static AtorAuditoria deAutenticacao(Authentication autenticacao) {
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            return sistema();
        }

        UUID usuarioId = null;
        String nome = null;
        if (autenticacao.getDetails() instanceof DadosUsuarioToken dados) {
            usuarioId = dados.usuarioId();
            nome = dados.nome();
        }
        if (nome == null || nome.isBlank()) {
            nome = autenticacao.getName();
        }

        String perfil = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(PREFIXO_ROLE))
                .map(a -> a.substring(PREFIXO_ROLE.length()))
                .findFirst()
                .orElse("DESCONHECIDO");

        return new AtorAuditoria(usuarioId, nome, perfil, OrigemAcao.PAINEL_WEB);
    }
}
