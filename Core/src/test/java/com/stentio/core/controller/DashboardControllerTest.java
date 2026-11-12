package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.DashboardPendenciasResponse;
import com.stentio.core.dto.DashboardResumoResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.DashboardService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    private static final String BASE_PATH = "/api/v1/dashboard";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("GET /api/v1/dashboard/pendencias retorna a lista contratada")
    void deveRetornarPendenciasComLinksDeAcao() throws Exception {
        UUID etapaId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        UUID orcamentoId = UUID.randomUUID();
        Instant prazoEtapa = Instant.parse("2026-10-20T18:00:00Z");
        Instant expiracaoOrcamento = Instant.parse("2026-10-22T23:59:59Z");

        when(dashboardService.pendencias()).thenReturn(new DashboardPendenciasResponse(2, List.of(
                new DashboardPendenciasResponse.Pendencia(
                        etapaId,
                        DashboardPendenciasResponse.TipoEntidade.ETAPA_OS,
                        "Tradução Técnica - OS-2026-A1B2C3",
                        "ATRASADO",
                        prazoEtapa,
                        "/ordens/" + ordemId),
                new DashboardPendenciasResponse.Pendencia(
                        orcamentoId,
                        DashboardPendenciasResponse.TipoEntidade.ORCAMENTO,
                        "Orçamento ORC-2026-D4E5F6 - Cliente ACME",
                        "EM_ELABORACAO",
                        expiracaoOrcamento,
                        "/orcamentos/" + orcamentoId))));

        mockMvc.perform(get(BASE_PATH + "/pendencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_itens").value(2))
                .andExpect(jsonPath("$.pendencias.length()").value(2))
                .andExpect(jsonPath("$.pendencias[0].id").value(etapaId.toString()))
                .andExpect(jsonPath("$.pendencias[0].tipo_entidade").value("ETAPA_OS"))
                .andExpect(jsonPath("$.pendencias[0].status").value("ATRASADO"))
                .andExpect(jsonPath("$.pendencias[0].data_limite").value("2026-10-20T18:00:00Z"))
                .andExpect(jsonPath("$.pendencias[0].link_acao").value("/ordens/" + ordemId))
                .andExpect(jsonPath("$.pendencias[1].id").value(orcamentoId.toString()))
                .andExpect(jsonPath("$.pendencias[1].tipo_entidade").value("ORCAMENTO"))
                .andExpect(jsonPath("$.pendencias[1].status").value("EM_ELABORACAO"))
                .andExpect(jsonPath("$.pendencias[1].data_limite").value("2026-10-22T23:59:59Z"))
                .andExpect(jsonPath("$.pendencias[1].link_acao").value("/orcamentos/" + orcamentoId));
    }

    @Test
    @WithMockUser(username = "admin@stentio.com", roles = {"ADMIN"})
    @DisplayName("ADMIN pode acessar o resumo financeiro do dashboard")
    void adminPodeAcessarResumo() throws Exception {
        when(dashboardService.resumo()).thenReturn(new DashboardResumoResponse(5, 3, 12, 2, 68.5));

        mockMvc.perform(get(BASE_PATH + "/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_orcamentos_em_elaboracao").value(5))
                .andExpect(jsonPath("$.total_orcamentos_aguardando_aprovacao").value(3))
                .andExpect(jsonPath("$.total_os_em_andamento").value(12))
                .andExpect(jsonPath("$.total_etapas_atrasadas").value(2))
                .andExpect(jsonPath("$.taxa_conversao_orcamentos").value(68.5));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("GESTOR_PROJETO pode acessar o resumo financeiro do dashboard")
    void gestorPodeAcessarResumo() throws Exception {
        when(dashboardService.resumo()).thenReturn(new DashboardResumoResponse(5, 3, 12, 2, 68.5));

        mockMvc.perform(get(BASE_PATH + "/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxa_conversao_orcamentos").value(68.5));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("ATENDENTE não pode acessar nenhuma rota do dashboard")
    void atendenteNaoPodeAcessarDashboard() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/resumo"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_PATH + "/pendencias"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(username = "financeiro@stentio.com", roles = {"FINANCEIRO"})
    @DisplayName("FINANCEIRO sem acesso ao dashboard recebe 403")
    void financeiroNaoPodeAcessarDashboard() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/resumo"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_PATH + "/pendencias"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Usuário anônimo não pode acessar nenhuma rota do dashboard")
    void anonimoNaoPodeAcessarDashboard() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/resumo"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_PATH + "/pendencias"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

}
