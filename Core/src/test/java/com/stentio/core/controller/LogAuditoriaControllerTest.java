package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.dto.LogAuditoriaPaginaResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.OrigemAcao;
import com.stentio.core.service.LogAuditoriaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// US-019 T6: contrato da API, RN.1 (sem alteração/exclusão) e RN.2 (só ADMIN).
@WebMvcTest(controllers = LogAuditoriaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LogAuditoriaControllerTest {

    private static final String CAMINHO = "/api/v1/auditoria/logs";
    private static final UUID LOG_ID = UUID.randomUUID();
    private static final UUID ETAPA_ID = UUID.randomUUID();
    private static final UUID GESTOR_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogAuditoriaService service;

    @MockitoBean
    private TokenService tokenService;

    private static LogAuditoriaPaginaResponse umaPagina() {
        return new LogAuditoriaPaginaResponse(1, 1, 20, 1, List.of(new LogAuditoriaPaginaResponse.Log(
                LOG_ID, EntidadeAuditada.ORDEM_SERVICO_ETAPA, ETAPA_ID,
                "OS OS-2026-ABC123 - Etapa 1 (Tradução)", "PENDENTE", "ALOCADO",
                new LogAuditoriaPaginaResponse.Usuario(GESTOR_ID, "Carlos Mendes", "GESTOR_PROJETO"),
                OrigemAcao.PAINEL_WEB, Instant.parse("2026-11-14T14:32:10Z"),
                "Recurso Ana Silva alocado à etapa.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cenário 1: Administrador consulta e recebe 200 no formato do contrato")
    void adminConsulta() throws Exception {
        when(service.listar(any(), anyInt(), anyInt())).thenReturn(umaPagina());

        mockMvc.perform(get(CAMINHO).param("entidade", "ORDEM_SERVICO_ETAPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_registros").value(1))
                .andExpect(jsonPath("$.pagina_atual").value(1))
                .andExpect(jsonPath("$.logs[0].id").value(LOG_ID.toString()))
                .andExpect(jsonPath("$.logs[0].entidade").value("ORDEM_SERVICO_ETAPA"))
                .andExpect(jsonPath("$.logs[0].entidade_id").value(ETAPA_ID.toString()))
                .andExpect(jsonPath("$.logs[0].referencia_legivel").value("OS OS-2026-ABC123 - Etapa 1 (Tradução)"))
                .andExpect(jsonPath("$.logs[0].status_anterior").value("PENDENTE"))
                .andExpect(jsonPath("$.logs[0].status_novo").value("ALOCADO"))
                .andExpect(jsonPath("$.logs[0].usuario.id").value(GESTOR_ID.toString()))
                .andExpect(jsonPath("$.logs[0].usuario.nome").value("Carlos Mendes"))
                .andExpect(jsonPath("$.logs[0].usuario.perfil").value("GESTOR_PROJETO"))
                .andExpect(jsonPath("$.logs[0].origem_acao").value("PAINEL_WEB"))
                .andExpect(jsonPath("$.logs[0].data_hora").value("2026-11-14T14:32:10Z"))
                .andExpect(jsonPath("$.logs[0].observacao").value("Recurso Ana Silva alocado à etapa."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CA.3: todos os filtros e a paginação chegam ao service")
    void repassaFiltros() throws Exception {
        when(service.listar(any(), anyInt(), anyInt())).thenReturn(umaPagina());
        UUID usuario = UUID.randomUUID();

        mockMvc.perform(get(CAMINHO)
                        .param("entidade", "ORCAMENTO")
                        .param("entidade_id", ETAPA_ID.toString())
                        .param("usuario_id", usuario.toString())
                        .param("data_inicio", "2026-11-01T00:00:00Z")
                        .param("data_fim", "2026-11-30T23:59:59Z")
                        .param("busca", "ORC-2026")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<LogAuditoriaFiltro> captor = ArgumentCaptor.forClass(LogAuditoriaFiltro.class);
        verify(service).listar(captor.capture(), eq(2), eq(10));
        LogAuditoriaFiltro filtro = captor.getValue();
        assertEquals(EntidadeAuditada.ORCAMENTO, filtro.entidade());
        assertEquals(ETAPA_ID, filtro.entidadeId());
        assertEquals(usuario, filtro.usuarioId());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), filtro.dataInicio());
        assertEquals(Instant.parse("2026-11-30T23:59:59Z"), filtro.dataFim());
        assertEquals("ORC-2026", filtro.busca());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Sem parâmetros: primeira página com 20 itens")
    void paginacaoPadrao() throws Exception {
        when(service.listar(any(), anyInt(), anyInt())).thenReturn(umaPagina());

        mockMvc.perform(get(CAMINHO)).andExpect(status().isOk());

        verify(service).listar(any(), eq(1), eq(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GESTOR_PROJETO", "ATENDENTE", "FINANCEIRO"})
    @DisplayName("Cenário 2 / RN.2: perfis que não são ADMIN recebem 403")
    void naoAdminRecebe403(String perfil) throws Exception {
        mockMvc.perform(get(CAMINHO).with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("usuario@stentio.com").roles(perfil)))
                .andExpect(status().isForbidden());

        verify(service, never()).listar(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Sem login recebe 401")
    void semLogin() throws Exception {
        mockMvc.perform(get(CAMINHO)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("RN.1: não existe rota para alterar ou excluir registros (405)")
    void semAlteracaoNemExclusao() throws Exception {
        mockMvc.perform(delete(CAMINHO)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(CAMINHO)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Entidade fora do enum responde 400")
    void entidadeInvalida() throws Exception {
        mockMvc.perform(get(CAMINHO).param("entidade", "CLIENTE"))
                .andExpect(status().isBadRequest());
    }
}
