package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.AceiteEtapaService;
import com.stentio.core.service.PortalRecursoService;
import com.stentio.core.service.RecusaEtapaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-023 CA.7: quem entra no portal e quem não entra.
 *
 * <p>O teste do atendente parece estranho à primeira vista — por que impedir alguém da
 * equipe? Porque o portal responde "as minhas demandas", e uma pessoa da equipe não é um
 * recurso: a pergunta não faz sentido para ela, e deixar passar significaria resolver um
 * perfil que não existe.
 */
@WebMvcTest(controllers = PortalRecursoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PortalRecursoControllerTest {

    private static final String ETAPAS = "/api/v1/recurso/etapas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortalRecursoService service;

    @MockitoBean
    private AceiteEtapaService aceiteEtapaService;

    @MockitoBean
    private RecusaEtapaService recusaEtapaService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("CA.5: o profissional vê as próprias demandas")
    void recursoAcessaOPortal() throws Exception {
        when(service.minhasDemandas(any())).thenReturn(List.of());

        mockMvc.perform(get(ETAPAS)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sem token responde 401")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get(ETAPAS)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("O portal é do recurso: atendente leva 403")
    void atendenteNaoEntraNoPortal() throws Exception {
        mockMvc.perform(get(ETAPAS)).andExpect(status().isForbidden());

        verify(service, never()).minhasDemandas(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Nem o administrador entra pelo portal — ele tem as telas da equipe")
    void adminNaoEntraNoPortal() throws Exception {
        mockMvc.perform(get(ETAPAS)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("RN.3: conta sem perfil responde 400 com explicação")
    void contaSemPerfilResponde400() throws Exception {
        when(service.minhasDemandas(any()))
                .thenThrow(new AutoCadastroInvalidoException("Esta conta ainda não tem perfil de recurso"));

        mockMvc.perform(get(ETAPAS)).andExpect(status().isBadRequest());
    }
}
