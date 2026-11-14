package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.exception.EtapasBloqueadasException;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.model.SituacaoEtapa;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.service.EtapaOrdemServicoService;
import com.stentio.core.service.TokenService;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EtapaOrdemServicoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EtapaOrdemServicoControllerTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final UUID TIPO_ID = UUID.randomUUID();
    private static final String BASE_PATH = "/api/v1/ordens-servico/" + OS_ID + "/etapas";

    private static final String CORPO_VALIDO = """
            {"etapas":[
              {"ordem":1,"nome":"Tradução Principal","tipo_servico_id":"%s","prazo_estimado_horas":24},
              {"ordem":2,"nome":"Revisão Final","tipo_servico_id":"%s","prazo_estimado_horas":12}
            ]}
            """.formatted(TIPO_ID, TIPO_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtapaOrdemServicoService service;

    @MockitoBean
    private TokenService tokenService;

    private static EtapasOrdemServicoResponse resposta() {
        return new EtapasOrdemServicoResponse(OS_ID, StatusOrdemServico.EM_PLANEJAMENTO, List.of(
                new EtapaOrdemServicoResponse(UUID.randomUUID(), 1, "Tradução Principal", TIPO_ID, "Tradução",
                        SituacaoEtapa.PENDENTE, 24, null, null, null),
                new EtapaOrdemServicoResponse(UUID.randomUUID(), 2, "Revisão Final", TIPO_ID, "Revisão",
                        SituacaoEtapa.AGUARDANDO_ETAPA_ANTERIOR, 12, null, null, null)));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Cenário 1: grava a sequência e devolve 200 com a árvore montada")
    void gravaSequencia() throws Exception {
        when(service.definirManualmente(eq(OS_ID), any())).thenReturn(resposta());

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordem_servico_id").value(OS_ID.toString()))
                .andExpect(jsonPath("$.status_os").value("EM_PLANEJAMENTO"))
                .andExpect(jsonPath("$.etapas[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.etapas[0].prazo_estimado_horas").value(24))
                .andExpect(jsonPath("$.etapas[0].tipo_servico_nome").value("Tradução"))
                .andExpect(jsonPath("$.etapas[1].status").value("AGUARDANDO_ETAPA_ANTERIOR"));

        var captor = ArgumentCaptor.forClass(EtapasOrdemServicoRequest.class);
        verify(service).definirManualmente(eq(OS_ID), captor.capture());
        assertEquals(2, captor.getValue().etapas().size());
        assertEquals(TIPO_ID, captor.getValue().etapas().getFirst().tipoServicoId());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("CA.2: aplicar modelo devolve a sequência clonada")
    void aplicaModelo() throws Exception {
        UUID modeloId = UUID.randomUUID();
        when(service.aplicarModelo(OS_ID, modeloId)).thenReturn(resposta());

        mockMvc.perform(post(BASE_PATH + "/aplicar-modelo/" + modeloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Lista as etapas da OS")
    void lista() throws Exception {
        when(service.listar(OS_ID)).thenReturn(resposta());

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas[0].ordem").value(1));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Lista de etapas vazia é recusada na validação, sem chegar ao service")
    void recusaCorpoVazio() throws Exception {
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("{\"etapas\":[]}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).definirManualmente(any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("RN.2: prazo zero é recusado na validação do contrato")
    void recusaPrazoZero() throws Exception {
        String corpo = """
                {"etapas":[{"ordem":1,"nome":"Tradução","tipo_servico_id":"%s","prazo_estimado_horas":0}]}
                """.formatted(TIPO_ID);

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());

        verify(service, never()).definirManualmente(any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("RN.1: lacuna na sequência volta 400 com a mensagem do domínio")
    void recusaLacuna() throws Exception {
        when(service.definirManualmente(eq(OS_ID), any()))
                .thenThrow(new EtapasInvalidasException("A sequência precisa começar em 1 e seguir sem lacunas; faltou a ordem 2"));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("lacunas")));
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("RN.3: sequência já alocada volta 409")
    void recusaSequenciaAlocada() throws Exception {
        when(service.definirManualmente(eq(OS_ID), any()))
                .thenThrow(new EtapasBloqueadasException("A sequência já tem etapa alocada a um recurso e não pode ser alterada"));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("Ordem de serviço inexistente volta 404")
    void recusaOrdemInexistente() throws Exception {
        when(service.definirManualmente(eq(OS_ID), any()))
                .thenThrow(new OrdemServicoNaoEncontradaException(OS_ID));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sem token é 401")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Perfil sem permissão é 403")
    void exigePerfil() throws Exception {
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isForbidden());
    }
}
