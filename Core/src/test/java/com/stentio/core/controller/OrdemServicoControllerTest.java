package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.OrdemServicoDetalheResponse;
import com.stentio.core.dto.OrdemServicoPaginaResponse;
import com.stentio.core.dto.OrdemServicoResumoResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.OrdemServicoInvalidaException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.service.OrdemServicoConsultaService;
import com.stentio.core.service.TokenService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrdemServicoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrdemServicoControllerTest {

    private static final String BASE_PATH = "/api/v1/ordens-servico";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoConsultaService ordemServicoConsultaService;

    @MockitoBean
    private TokenService tokenService;

    private final UUID ordemId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    private OrdemServicoPaginaResponse pagina() {
        OrdemServicoResumoResponse resumo = new OrdemServicoResumoResponse(
                ordemId,
                "OS-2026-0001",
                StatusOrdemServico.PENDENTE_ALOCACAO,
                clienteId,
                "Empresa XPTO",
                Instant.parse("2026-10-09T08:00:00Z"));

        return new OrdemServicoPaginaResponse(1, 10, 1, List.of(resumo));
    }

    private OrdemServicoDetalheResponse detalhe() {
        UUID solicitacaoId = UUID.randomUUID();
        UUID arquivoId = UUID.randomUUID();

        return new OrdemServicoDetalheResponse(
                ordemId,
                "OS-2026-0001",
                StatusOrdemServico.PENDENTE_ALOCACAO,
                UUID.randomUUID(),
                Instant.parse("2026-10-09T08:00:00Z"),
                new OrdemServicoDetalheResponse.Cliente(clienteId, "Empresa XPTO"),
                new OrdemServicoDetalheResponse.Solicitacao(
                        solicitacaoId,
                        "Contrato recebido por e-mail",
                        "Tradução Juramentada",
                        "Português",
                        "Inglês",
                        List.of(new OrdemServicoDetalheResponse.Arquivo(
                                arquivoId,
                                "contrato.pdf",
                                "/api/v1/solicitacoes/" + solicitacaoId + "/arquivos/" + arquivoId))));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Cenário 1: deve listar filtrando por status")
    void deveListarFiltrandoPorStatus() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        mockMvc.perform(get(BASE_PATH).param("status", "PENDENTE_ALOCACAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho_pagina").value(10))
                .andExpect(jsonPath("$.total_elementos").value(1))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(ordemId.toString()))
                .andExpect(jsonPath("$.conteudo[0].codigo_visual").value("OS-2026-0001"))
                .andExpect(jsonPath("$.conteudo[0].status").value("PENDENTE_ALOCACAO"))
                .andExpect(jsonPath("$.conteudo[0].cliente_nome").value("Empresa XPTO"))
                .andExpect(jsonPath("$.conteudo[0].data_criacao").value(Matchers.startsWith("2026-10-09")));

        verify(ordemServicoConsultaService)
                .listar(eq(StatusOrdemServico.PENDENTE_ALOCACAO), eq(null), eq(1), eq(10));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Cenário 1b: deve listar filtrando por cliente_id")
    void deveListarFiltrandoPorCliente() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        mockMvc.perform(get(BASE_PATH).param("cliente_id", clienteId.toString()))
                .andExpect(status().isOk());

        verify(ordemServicoConsultaService).listar(eq(null), eq(clienteId), eq(1), eq(10));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Cenário 1b: deve aceitar cliente como alias de cliente_id")
    void deveListarFiltrandoPorAliasCliente() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        mockMvc.perform(get(BASE_PATH).param("cliente", clienteId.toString()))
                .andExpect(status().isOk());

        verify(ordemServicoConsultaService).listar(eq(null), eq(clienteId), eq(1), eq(10));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Cenário 1b: deve priorizar cliente_id quando ambos os nomes vêm preenchidos")
    void devePriorizarClienteIdSobreAlias() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        UUID outroCliente = UUID.randomUUID();
        mockMvc.perform(get(BASE_PATH)
                        .param("cliente_id", clienteId.toString())
                        .param("cliente", outroCliente.toString()))
                .andExpect(status().isOk());

        verify(ordemServicoConsultaService).listar(eq(null), eq(clienteId), eq(1), eq(10));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("CA.2: deve repassar pagina e tamanho_pagina recebidos na URL")
    void deveRepassarPaginacao() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        mockMvc.perform(get(BASE_PATH).param("pagina", "3").param("tamanho_pagina", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Integer> paginaCapturada = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> tamanhoCapturado = ArgumentCaptor.forClass(Integer.class);
        verify(ordemServicoConsultaService)
                .listar(any(), any(), paginaCapturada.capture(), tamanhoCapturado.capture());

        Assertions.assertEquals(3, paginaCapturada.getValue());
        Assertions.assertEquals(5, tamanhoCapturado.getValue());
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Cenário 2: deve retornar os detalhes e os vínculos da OS")
    void deveRetornarDetalhesDaOrdem() throws Exception {
        when(ordemServicoConsultaService.buscarPorId(eq(ordemId))).thenReturn(detalhe());

        mockMvc.perform(get(BASE_PATH + "/" + ordemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ordemId.toString()))
                .andExpect(jsonPath("$.codigo").value("OS-2026-0001"))
                .andExpect(jsonPath("$.status").value("PENDENTE_ALOCACAO"))
                .andExpect(jsonPath("$.orcamento_id").exists())
                .andExpect(jsonPath("$.cliente.id").value(clienteId.toString()))
                .andExpect(jsonPath("$.cliente.nome").value("Empresa XPTO"))
                .andExpect(jsonPath("$.solicitacao.categoria").value("Tradução Juramentada"))
                .andExpect(jsonPath("$.solicitacao.idioma_origem").value("Português"))
                .andExpect(jsonPath("$.solicitacao.idioma_destino").value("Inglês"))
                .andExpect(jsonPath("$.solicitacao.arquivos.length()").value(1))
                .andExpect(jsonPath("$.solicitacao.arquivos[0].nome").value("contrato.pdf"))
                // O link do anexo é o download autenticado da US-09, não um caminho em disco.
                .andExpect(jsonPath("$.solicitacao.arquivos[0].url")
                        .value(Matchers.startsWith("/api/v1/solicitacoes/")));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Deve retornar 400 quando a página é inválida")
    void deveRetornarBadRequestComPaginaInvalida() throws Exception {
        when(ordemServicoConsultaService.listar(any(), any(), anyInt(), anyInt()))
                .thenThrow(new OrdemServicoInvalidaException("A página deve ser maior ou igual a 1"));

        mockMvc.perform(get(BASE_PATH).param("pagina", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A página deve ser maior ou igual a 1"));
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Deve retornar 404 quando a OS não existe")
    void deveRetornarNotFound() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(ordemServicoConsultaService.buscarPorId(eq(inexistente)))
                .thenThrow(new OrdemServicoNaoEncontradaException(inexistente));

        mockMvc.perform(get(BASE_PATH + "/" + inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de serviço não encontrada: " + inexistente));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve retornar 403 para perfil sem permissão")
    void deveRetornarForbidden() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isForbidden());

        verify(ordemServicoConsultaService, never()).listar(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Deve retornar 401 sem token")
    void deveRetornarUnauthorized() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isUnauthorized());
    }
}
