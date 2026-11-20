package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.EntregaEtapaResponse;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.service.AceiteEtapaService;
import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.EntregaEtapaService;
import com.stentio.core.service.PortalRecursoService;
import com.stentio.core.service.RecusaEtapaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private static final String ACEITAR = ETAPAS + "/" + UUID.randomUUID() + "/aceitar";
    private static final UUID ETAPA_ID = UUID.randomUUID();
    private static final UUID PROXIMA_ETAPA_ID = UUID.randomUUID();
    private static final UUID ORDEM_SERVICO_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortalRecursoService service;

    @MockitoBean
    private AceiteEtapaService aceiteEtapaService;

    @MockitoBean
    private RecusaEtapaService recusaEtapaService;

    @MockitoBean
    private EntregaEtapaService entregaEtapaService;

    @MockitoBean
    private ArquivoStorageService arquivoStorageService;

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

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("CA.3: o profissional aceita pela própria rota do portal")
    void recursoAceita() throws Exception {
        mockMvc.perform(post(ACEITAR)).andExpect(status().isOk());

        verify(aceiteEtapaService).aceitar(any(), any());
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("aceite que perde a disputa retorna 409")
    void aceiteConcorrenteRetornaConflict() throws Exception {
        when(aceiteEtapaService.aceitar(any(), any()))
                .thenThrow(new TransicaoEtapaInvalidaException("Esta etapa já foi aceita"));

        mockMvc.perform(post(ACEITAR)).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("ninguém da equipe aceita etapa no lugar de um profissional")
    void atendenteNaoAceita() throws Exception {
        mockMvc.perform(post(ACEITAR)).andExpect(status().isForbidden());

        verify(aceiteEtapaService, never()).aceitar(any(), any());
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("a entrega devolve a URL de download e aceita os dois caminhos documentados")
    void entregaDevolveUrl() throws Exception {
        when(entregaEtapaService.entregar(any(), any(), any())).thenReturn(
                new EntregaEtapaResponse(
                        ETAPA_ID,
                        StatusEtapaOrdemServico.ENTREGUE,
                        Instant.parse("2026-11-20T12:00:00Z"),
                        "traducao.docx",
                        "/api/v1/recurso/etapas/" + ETAPA_ID + "/entrega/arquivo",
                        new EntregaEtapaResponse.Avanco(
                                PROXIMA_ETAPA_ID, StatusEtapaOrdemServico.DISPARADA, true, 3),
                        new EntregaEtapaResponse.OrdemServico(
                                ORDEM_SERVICO_ID, StatusOrdemServico.EM_EXECUCAO)));
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "traducao.docx", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(MockMvcRequestBuilders.multipart(ETAPAS + "/" + ETAPA_ID + "/entregar").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa_id").value(ETAPA_ID.toString()))
                .andExpect(jsonPath("$.arquivo_nome").value("traducao.docx"))
                .andExpect(jsonPath("$.arquivo_url")
                        .value("/api/v1/recurso/etapas/" + ETAPA_ID + "/entrega/arquivo"))
                .andExpect(jsonPath("$.avanco.proxima_etapa_id").value(PROXIMA_ETAPA_ID.toString()))
                .andExpect(jsonPath("$.avanco.proxima_etapa_status").value("DISPARADA"))
                .andExpect(jsonPath("$.avanco.convite_automatico").value(true))
                .andExpect(jsonPath("$.avanco.recursos_notificados").value(3))
                .andExpect(jsonPath("$.ordem_servico.id").value(ORDEM_SERVICO_ID.toString()))
                .andExpect(jsonPath("$.ordem_servico.status").value("EM_EXECUCAO"));

        mockMvc.perform(MockMvcRequestBuilders.multipart(ETAPAS + "/" + ETAPA_ID + "/entrega").file(arquivo))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("só o recurso autenticado pode baixar a própria entrega")
    void recursoBaixaPropriaEntrega() throws Exception {
        when(entregaEtapaService.localizarEntregaDoRecurso(any(), any()))
                .thenReturn(new EntregaEtapaService.ArquivoEntregue("traducao.docx", "2026/11/id.docx"));
        when(arquivoStorageService.carregar("2026/11/id.docx")).thenReturn(mock(Resource.class));

        mockMvc.perform(get(ETAPAS + "/" + ETAPA_ID + "/entrega/arquivo"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("a equipe não usa o endpoint de download privado do portal")
    void equipeNaoBaixaEntregaPeloPortal() throws Exception {
        mockMvc.perform(get(ETAPAS + "/" + ETAPA_ID + "/entrega/arquivo"))
                .andExpect(status().isForbidden());

        verify(entregaEtapaService, never()).localizarEntregaDoRecurso(any(), any());
    }
}
