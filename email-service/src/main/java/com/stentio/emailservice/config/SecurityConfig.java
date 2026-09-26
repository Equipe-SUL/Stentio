package com.stentio.emailservice.config;

import com.stentio.emailservice.model.Role;
import com.stentio.emailservice.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROTA_EMPRESA = "/api/v1/empresa";
    private static final String ROTAS_EMPRESA = "/api/v1/empresa/**";

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
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Empresa: leitura para qualquer usuário autenticado, escrita restrita ao ADMIN.
                        .requestMatchers(HttpMethod.GET, ROTA_EMPRESA, ROTAS_EMPRESA).authenticated()
                        .requestMatchers(ROTA_EMPRESA, ROTAS_EMPRESA).hasRole(Role.ADMIN.name())

                        // SMTP e envio de e-mail também exigem sessão; alterar a configuração é do ADMIN.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/smtp",
                                "/api/v1/smtp/**"
                        ).authenticated()
                        .requestMatchers("/api/v1/smtp", "/api/v1/smtp/**").hasRole(Role.ADMIN.name())

                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .build();
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
