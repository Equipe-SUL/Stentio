package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.AlocacaoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.AlocacaoNaoPermitidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.SituacaoEtapa;
import com.stentio.core.service.AlocacaoEtapaService;
import com.stentio.core.service.TokenService;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlocacaoEtapaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AlocacaoEtapaControllerTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID ETAPA_ID = UUID.randomUUID();
    private static final UUID RECURSO_ID = UUID.randomUUID();
    private static final Instant LIMITE = Instant.parse("2026-12-25T18:00:00Z");

    private static final String CAMINHO =
            "/api/v1/ordens-servico/" + OS_ID + "/etapas/" + ETAPA_ID + "/alocacao";

    private static final String CORPO_VALIDO = """
            {"recurso_id":"%s","data_limite_entrega":"2026-12-25T18:00:00Z"}
            """.formatted(RECURSO_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlocacaoEtapaService service;

    @MockitoBean
    private TokenService tokenService;

    private static AlocacaoEtapaResponse resposta() {
        return new AlocacaoEtapaResponse(ETAPA_ID, SituacaoEtapa.ALOCADO,
                new AlocacaoEtapaResponse.Recurso(RECURSO_ID, "Ana Silva"), LIMITE);
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Cenário 1: aloca e devolve 200 no formato do contrato")
    void alocaComSucesso() throws Exception {
        when(service.alocar(eq(OS_ID), eq(ETAPA_ID), any(), any())).thenReturn(resposta());

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa_id").value(ETAPA_ID.toString()))
                .andExpect(jsonPath("$.status_etapa").value("ALOCADO"))
                .andExpect(jsonPath("$.recurso.id").value(RECURSO_ID.toString()))
                .andExpect(jsonPath("$.recurso.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.data_limite_entrega").value("2026-12-25T18:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("O corpo chega ao service em snake_case, com a data lida como instante")
    void traduzOCorpoDoContrato() throws Exception {
        when(service.alocar(any(), any(), any(), any())).thenReturn(resposta());

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isOk());

        ArgumentCaptor<AlocacaoEtapaRequest> corpo = ArgumentCaptor.forClass(AlocacaoEtapaRequest.class);
        verify(service).alocar(eq(OS_ID), eq(ETAPA_ID), corpo.capture(), any());

        assertEquals(RECURSO_ID, corpo.getValue().recursoId());
        assertEquals(LIMITE, corpo.getValue().dataLimiteEntrega());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("O gestor autenticado chega ao service sem perfil administrativo")
    void identificaOAtor() throws Exception {
        when(service.alocar(any(), any(), any(), any())).thenReturn(resposta());

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isOk());

        ArgumentCaptor<UsuarioAutenticado> ator = ArgumentCaptor.forClass(UsuarioAutenticado.class);
        verify(service).alocar(any(), any(), any(), ator.capture());

        assertEquals("user", ator.getValue().email());
        assertFalse(ator.getValue().administrador());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("O administrador chega ao service marcado como tal, que é o que a RN.3 usa")
    void identificaOAdministrador() throws Exception {
        when(service.alocar(any(), any(), any(), any())).thenReturn(resposta());

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isOk());

        ArgumentCaptor<UsuarioAutenticado> ator = ArgumentCaptor.forClass(UsuarioAutenticado.class);
        verify(service).alocar(any(), any(), any(), ator.capture());

        assertTrue(ator.getValue().administrador());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Cenário 2: incompatibilidade de perfil responde 400 com a mensagem do domínio")
    void incompatibilidadeResponde400() throws Exception {
        when(service.alocar(any(), any(), any(), any()))
                .thenThrow(new AlocacaoInvalidaException("Ana Silva não tem o par Português → Inglês nas competências"));

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ana Silva não tem o par Português → Inglês nas competências"));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("RN.3: etapa iniciada sem perfil administrativo responde 403")
    void etapaIniciadaResponde403() throws Exception {
        when(service.alocar(any(), any(), any(), any()))
                .thenThrow(new AlocacaoNaoPermitidaException("A etapa Tradução Principal já está em andamento"));

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Etapa inexistente responde 404")
    void etapaInexistenteResponde404() throws Exception {
        when(service.alocar(any(), any(), any(), any()))
                .thenThrow(new EtapaNaoEncontradaException(ETAPA_ID));

        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Corpo sem recurso_id responde 400 sem chegar ao service")
    void corpoSemRecursoResponde400() throws Exception {
        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data_limite_entrega\":\"2026-12-25T18:00:00Z\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).alocar(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Corpo sem data limite responde 400 sem chegar ao service")
    void corpoSemDataResponde400() throws Exception {
        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recurso_id\":\"" + RECURSO_ID + "\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).alocar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Sem token responde 401")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Alocar não é do atendente: responde 403")
    void atendenteResponde403() throws Exception {
        mockMvc.perform(post(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isForbidden());

        verify(service, never()).alocar(any(), any(), any(), any());
    }
}
