package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.OrcamentoCriacaoRequest;
import com.stentio.core.dto.OrcamentoDetalheResponse;
import com.stentio.core.dto.OrcamentoItensRequest;
import com.stentio.core.dto.OrcamentoPaginaResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEditavelException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.SolicitacaoNaoEncontradaException;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.service.OrcamentoService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrcamentoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrcamentoControllerTest {

    private static final String BASE_PATH = "/api/v1/orcamentos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrcamentoService orcamentoService;

    @MockitoBean
    private TokenService tokenService;

    private final UUID orcamentoId = UUID.randomUUID();
    private final UUID solicitacaoId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final UUID idiomaOrigemId = UUID.randomUUID();
    private final UUID idiomaDestinoId = UUID.randomUUID();

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 1: POST /api/v1/orcamentos deve retornar 201 e dados do orçamento criado")
    void deveCriarOrcamentoComSucesso() throws Exception {
        Instant expiracao = Instant.parse("2026-10-25T23:59:59Z");
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(solicitacaoId, clienteId, expiracao);

        OrcamentoDetalheResponse response = new OrcamentoDetalheResponse(
                orcamentoId, 1, clienteId, solicitacaoId,
                StatusOrcamento.EM_ELABORACAO, new BigDecimal("0.00"), expiracao, List.of()
        );

        when(orcamentoService.criar(any(OrcamentoCriacaoRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_PATH + "/" + orcamentoId))
                .andExpect(jsonPath("$.id").value(orcamentoId.toString()))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.status").value("EM_ELABORACAO"))
                .andExpect(jsonPath("$.valor_total").value(0.00))
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("POST /api/v1/orcamentos retorna 404 quando a solicitação não existe")
    void deveRetornar404QuandoSolicitacaoNaoExiste() throws Exception {
        OrcamentoCriacaoRequest request = new OrcamentoCriacaoRequest(
                solicitacaoId, clienteId, Instant.parse("2026-10-25T23:59:59Z"));

        when(orcamentoService.criar(any())).thenThrow(new SolicitacaoNaoEncontradaException(solicitacaoId));

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 1 e 5: POST /api/v1/orcamentos/{id}/itens deve substituir itens e retornar 201 com subtotais")
    void deveSubstituirItensComSucesso() throws Exception {
        OrcamentoItensRequest.Servico s1 = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("1500"), UnidadeCobranca.PALAVRA, new BigDecimal("0.25"));
        OrcamentoItensRequest.Servico s2 = new OrcamentoItensRequest.Servico(
                "REVISAO", new BigDecimal("15"), UnidadeCobranca.PAGINA, new BigDecimal("0.10"));
        OrcamentoItensRequest.Item item = new OrcamentoItensRequest.Item(
                1, idiomaOrigemId, idiomaDestinoId, List.of(s1, s2));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(item));

        OrcamentoDetalheResponse.Servico sDet1 = new OrcamentoDetalheResponse.Servico(
                UUID.randomUUID(), "TRADUCAO", new BigDecimal("1500.00"), UnidadeCobranca.PALAVRA,
                new BigDecimal("0.2500"), new BigDecimal("375.00"));
        OrcamentoDetalheResponse.Servico sDet2 = new OrcamentoDetalheResponse.Servico(
                UUID.randomUUID(), "REVISAO", new BigDecimal("15.00"), UnidadeCobranca.PAGINA,
                new BigDecimal("0.1000"), new BigDecimal("1.50"));
        OrcamentoDetalheResponse.Item itemDet = new OrcamentoDetalheResponse.Item(
                UUID.randomUUID(), 1, idiomaOrigemId, idiomaDestinoId, new BigDecimal("376.50"), List.of(sDet1, sDet2));

        OrcamentoDetalheResponse response = new OrcamentoDetalheResponse(
                orcamentoId, 1, clienteId, solicitacaoId,
                StatusOrcamento.EM_ELABORACAO, new BigDecimal("376.50"),
                Instant.parse("2026-10-25T23:59:59Z"), List.of(itemDet));

        when(orcamentoService.substituirItens(eq(orcamentoId), any(OrcamentoItensRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_PATH + "/" + orcamentoId + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_PATH + "/" + orcamentoId))
                .andExpect(jsonPath("$.valor_total").value(376.50))
                .andExpect(jsonPath("$.itens[0].subtotal_item").value(376.50))
                .andExpect(jsonPath("$.itens[0].servicos[0].subtotal").value(375.00))
                .andExpect(jsonPath("$.itens[0].servicos[1].subtotal").value(1.50));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 2: POST /api/v1/orcamentos/{id}/itens com origem = destino deve retornar 400 com mensagem exata")
    void deveRetornar400QuandoIdiomasIguais() throws Exception {
        OrcamentoItensRequest.Servico s = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("100"), UnidadeCobranca.PALAVRA, new BigDecimal("0.50"));
        OrcamentoItensRequest.Item item = new OrcamentoItensRequest.Item(
                1, idiomaOrigemId, idiomaOrigemId, List.of(s));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(item));

        when(orcamentoService.substituirItens(eq(orcamentoId), any()))
                .thenThrow(new OrcamentoInvalidoException("O idioma de origem não pode ser igual ao idioma de destino"));

        mockMvc.perform(post(BASE_PATH + "/" + orcamentoId + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O idioma de origem não pode ser igual ao idioma de destino"));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 3: POST /api/v1/orcamentos/{id}/itens com item sem serviços deve retornar 400")
    void deveRetornar400QuandoItemSemServicos() throws Exception {
        String json = """
                {
                  "itens": [
                    {
                      "ordem": 1,
                      "idioma_origem_id": "%s",
                      "idioma_destino_id": "%s",
                      "servicos": []
                    }
                  ]
                }
                """.formatted(idiomaOrigemId, idiomaDestinoId);

        mockMvc.perform(post(BASE_PATH + "/" + orcamentoId + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Todo item deve conter ao menos um serviço precificado"));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("POST /api/v1/orcamentos/{id}/itens retorna 409 quando orçamento não está em elaboração")
    void deveRetornar409QuandoOrcamentoNaoEditavel() throws Exception {
        OrcamentoItensRequest.Servico s = new OrcamentoItensRequest.Servico(
                "TRADUCAO", new BigDecimal("100"), UnidadeCobranca.PALAVRA, new BigDecimal("0.50"));
        OrcamentoItensRequest request = new OrcamentoItensRequest(List.of(
                new OrcamentoItensRequest.Item(1, idiomaOrigemId, idiomaDestinoId, List.of(s))));

        when(orcamentoService.substituirItens(eq(orcamentoId), any()))
                .thenThrow(new OrcamentoNaoEditavelException());

        mockMvc.perform(post(BASE_PATH + "/" + orcamentoId + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 4: GET /api/v1/orcamentos deve retornar 200 com paginação e formato snake_case")
    void deveListarOrcamentosPaginados() throws Exception {
        OrcamentoPaginaResponse.Resumo resumo = new OrcamentoPaginaResponse.Resumo(
                orcamentoId, 1, clienteId, solicitacaoId,
                StatusOrcamento.EM_ELABORACAO, new BigDecimal("525.00"),
                Instant.parse("2026-10-25T23:59:59Z"));
        OrcamentoPaginaResponse paginaResponse = new OrcamentoPaginaResponse(1, 10, 1, List.of(resumo));

        when(orcamentoService.listar(eq(StatusOrcamento.EM_ELABORACAO), any(), any(), eq(1), eq(10)))
                .thenReturn(paginaResponse);

        mockMvc.perform(get(BASE_PATH)
                        .param("status", "EM_ELABORACAO")
                        .param("pagina", "1")
                        .param("tamanho_pagina", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho_pagina").value(10))
                .andExpect(jsonPath("$.total_elementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(orcamentoId.toString()))
                .andExpect(jsonPath("$.conteudo[0].status").value("EM_ELABORACAO"))
                .andExpect(jsonPath("$.conteudo[0].valor_total").value(525.00));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 5: GET /api/v1/orcamentos/{id} deve retornar 200 com hierarquia completa")
    void deveConsultarDetalheDoOrcamento() throws Exception {
        OrcamentoDetalheResponse response = new OrcamentoDetalheResponse(
                orcamentoId, 1, clienteId, solicitacaoId,
                StatusOrcamento.EM_ELABORACAO, new BigDecimal("975.00"),
                Instant.parse("2026-10-25T23:59:59Z"), List.of()
        );

        when(orcamentoService.buscarPorId(orcamentoId)).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/" + orcamentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orcamentoId.toString()))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.valor_total").value(975.00));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("GET /api/v1/orcamentos/{id} inexistente deve retornar 404")
    void deveRetornar404QuandoBuscarIdInexistente() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(orcamentoService.buscarPorId(inexistente)).thenThrow(new OrcamentoNaoEncontradoException(inexistente));

        mockMvc.perform(get(BASE_PATH + "/" + inexistente))
                .andExpect(status().isNotFound());
    }
}
