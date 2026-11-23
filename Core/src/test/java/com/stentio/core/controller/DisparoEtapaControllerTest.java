package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.service.DisparoEtapaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-013 CA.1: disparar é decisão de quem gerencia o projeto.
 *
 * <p>O caso do profissional é o que importa aqui: ele é o destinatário do disparo, e
 * poder disparar para si mesmo contornaria a escolha do gestor.
 */
@WebMvcTest(controllers = DisparoEtapaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DisparoEtapaControllerTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID ETAPA_ID = UUID.randomUUID();
    private static final String CAMINHO =
            "/api/v1/ordens-servico/" + OS_ID + "/etapas/" + ETAPA_ID + "/disparo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisparoEtapaService service;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("o gestor dispara e recebe quantos foram notificados")
    void gestorDispara() throws Exception {
        when(service.disparar(any(), any(), any())).thenReturn(
                new DisparoEtapaResponse(ETAPA_ID, StatusEtapaOrdemServico.DISPARADA, 4));

        mockMvc.perform(post(CAMINHO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPARADA"))
                .andExpect(jsonPath("$.recursos_notificados").value(4));
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("o profissional não dispara etapas")
    void recursoNaoDispara() throws Exception {
        mockMvc.perform(post(CAMINHO)).andExpect(status().isForbidden());

        verify(service, never()).disparar(any(), any(), any());
    }

    @Test
    @DisplayName("sem autenticação não passa")
    void anonimoNaoDispara() throws Exception {
        mockMvc.perform(post(CAMINHO)).andExpect(status().isUnauthorized());
    }
}
