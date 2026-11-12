package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.DashboardPendenciasResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.DashboardService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
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

}
