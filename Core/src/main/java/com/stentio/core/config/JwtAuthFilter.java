package com.stentio.core.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.stentio.core.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Sem @Component de propósito: é instanciado no SecurityConfig para rodar só dentro da cadeia do Spring Security.
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String PREFIXO_ROLE = "ROLE_";
    // Mesmo nome de cookie definido no AuthCookieService do serviço Auth.
    private static final String COOKIE_TOKEN = "stentio_token";

    private final TokenService tokenService;

    public JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        obterToken(request).ifPresent(this::autenticar);
        filterChain.doFilter(request, response);
    }

    private void autenticar(String token) {
        try {
            TokenService.TokenDados dados = tokenService.validarToken(token);
            var perfil = new SimpleGrantedAuthority(PREFIXO_ROLE + dados.role().name());
            var autenticacao = new UsernamePasswordAuthenticationToken(dados.email(), null, List.of(perfil));
            // US-019: id e nome viajam em details, e não como principal, para getName()
            // continuar devolvendo o e-mail — é dele que dependem o histórico da US-012 e
            // todo @WithMockUser dos testes existentes.
            autenticacao.setDetails(new DadosUsuarioToken(dados.usuarioId(), dados.nome()));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            // Token inválido segue sem autenticação; o SecurityConfig responde 401.
            SecurityContextHolder.clearContext();
        }
    }

    // Header Bearer atende o app mobile; o cookie HttpOnly atende o front web.
    private Optional<String> obterToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            return naoVazio(header.substring(PREFIXO_BEARER.length()));
        }

        return Optional.ofNullable(WebUtils.getCookie(request, COOKIE_TOKEN))
                .map(cookie -> cookie.getValue())
                .flatMap(JwtAuthFilter::naoVazio);
    }

    private static Optional<String> naoVazio(String valor) {
        return Optional.ofNullable(valor).filter(v -> !v.isBlank());
    }
}
