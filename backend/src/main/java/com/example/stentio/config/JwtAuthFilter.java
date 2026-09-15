package com.example.stentio.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.stentio.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";

    private final TokenService tokenService;

    public JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length());
            try {
                TokenService.TokenDados dados = tokenService.validarToken(token);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + dados.role().name()));
                var authentication = new UsernamePasswordAuthenticationToken(dados.email(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JWTVerificationException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

}