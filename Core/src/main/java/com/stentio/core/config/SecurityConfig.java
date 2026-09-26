package com.stentio.core.config;

import com.stentio.core.model.Role;
import com.stentio.core.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROTAS_CLIENTES = "/api/v1/clientes/**";
    private static final String ROTAS_RECURSOS = "/api/v1/recursos/**";
    private static final String ROTAS_CATEGORIAS_PROJETO = "/api/v1/categorias-projeto/**";
    private static final String ROTAS_IDIOMAS = "/api/v1/idiomas/**";
    private static final String ROTAS_TIPOS_SERVICO = "/api/v1/tipos-servico/**";
    private static final String ROTAS_TABELAS_PRECO = "/api/v1/admin/tabelas-preco/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        return http
                // CSRF desligado: API stateless, e o cookie do Auth é SameSite=Lax, que já bloqueia POST/PUT/PATCH vindos de outros sites.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excecao -> excecao
                        .authenticationEntryPoint((request, response, ex) ->
                                escreverErro(response, HttpStatus.UNAUTHORIZED, "Token ausente ou inválido"))
                        .accessDeniedHandler((request, response, ex) ->
                                escreverErro(response, HttpStatus.FORBIDDEN, "Perfil sem permissão para esta operação"))
                )
                .authorizeHttpRequests(rotas -> rotas
                        // Liberação das rotas do Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Regra de clientes: somente a role mais alta (ADMIN) pode excluir clientes
                        .requestMatchers(HttpMethod.DELETE, ROTAS_CLIENTES).hasRole(Role.ADMIN.name())
                        // Qualquer usuário autenticado pode cadastrar, listar, consultar ou modificar clientes
                        .requestMatchers(ROTAS_CLIENTES).authenticated()
                        .requestMatchers(HttpMethod.GET, ROTAS_RECURSOS).authenticated()
                        .requestMatchers(ROTAS_RECURSOS).hasAnyRole(Role.ADMIN.name(), Role.GESTOR_PROJETO.name())
                        .requestMatchers(ROTAS_CATEGORIAS_PROJETO).hasAnyRole(Role.ADMIN.name(), Role.FINANCEIRO.name(), Role.GESTOR_PROJETO.name())
                        .requestMatchers(ROTAS_IDIOMAS).hasAnyRole(Role.ADMIN.name(), Role.FINANCEIRO.name(), Role.GESTOR_PROJETO.name())
                        .requestMatchers(ROTAS_TIPOS_SERVICO).hasAnyRole(Role.ADMIN.name(), Role.FINANCEIRO.name(), Role.GESTOR_PROJETO.name())
                        // CA.5 da US-05: só ADMIN e GESTOR_PROJETO gerenciam a tabela de preços.
                        .requestMatchers(ROTAS_TABELAS_PRECO).hasAnyRole(Role.ADMIN.name(), Role.GESTOR_PROJETO.name())
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origensPermitidas) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origensPermitidas.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Mesmo formato de erro do GlobalExceptionHandler, para o front tratar todas as falhas de um jeito só.
    private static void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s"}"""
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(), mensagem));
    }
}
