package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.dto.SolicitacaoResponse.ArquivoResumo;
import com.stentio.core.dto.SolicitacaoResponse.CategoriaProjetoResumo;
import com.stentio.core.dto.SolicitacaoResponse.ClienteResumo;
import com.stentio.core.dto.SolicitacaoResponse.IdiomaResumo;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.service.SolicitacaoService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SolicitacaoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SolicitacaoControllerTest {

    private static final String BASE_PATH = "/api/v1/solicitacoes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitacaoService solicitacaoService;

    @MockitoBean
    private TokenService tokenService;

    private final UUID clienteId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();
    private final UUID idiomaOrigemId = UUID.randomUUID();
    private final UUID idiomaDestinoId = UUID.randomUUID();

    private MockMultipartFile arquivoPdf() {
        return new MockMultipartFile(
                "arquivos", "documento.pdf", "application/pdf", "conteudo".getBytes(StandardCharsets.UTF_8));
    }

    private SolicitacaoResponse respostaCriada(UUID id) {
        ArquivoResumo arquivo = new ArquivoResumo(
                UUID.randomUUID(), "documento.pdf", BASE_PATH + "/" + id + "/arquivos/x", "application/pdf", 8L);

        return new SolicitacaoResponse(
                id,
                new ClienteResumo(clienteId, "Stentio Tecnologia"),
                new CategoriaProjetoResumo(categoriaId, "Jurídico"),
                new IdiomaResumo(idiomaOrigemId, "Inglês", "en"),
                new IdiomaResumo(idiomaDestinoId, "Português", "pt-BR"),
                "Tradução de contrato recebida por e-mail",
                StatusSolicitacao.PENDENTE_ORCAMENTO,
                LocalDateTime.now(),
                List.of(arquivo)
        );
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 1: deve criar solicitação com um arquivo e retornar 201")
    void deveCriarSolicitacaoComArquivo() throws Exception {
        UUID solicitacaoId = UUID.randomUUID();
        when(solicitacaoService.criar(any(), any())).thenReturn(respostaCriada(solicitacaoId));

        mockMvc.perform(multipart(BASE_PATH)
                        .file(arquivoPdf())
                        .param("clienteId", clienteId.toString())
                        .param("categoriaProjetoId", categoriaId.toString())
                        .param("idiomaOrigemId", idiomaOrigemId.toString())
                        .param("idiomaDestinoId", idiomaDestinoId.toString())
                        .param("descricao", "Tradução de contrato recebida por e-mail"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_PATH + "/" + solicitacaoId))
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()))
                // CA.4: o status vem do backend, não do formulário.
                .andExpect(jsonPath("$.status").value("PENDENTE_ORCAMENTO"))
                .andExpect(jsonPath("$.cliente.nomeEmpresa").value("Stentio Tecnologia"))
                .andExpect(jsonPath("$.categoriaProjeto.nome").value("Jurídico"))
                .andExpect(jsonPath("$.idiomaOrigem.codigoIso").value("en"))
                .andExpect(jsonPath("$.arquivos.length()").value(1))
                .andExpect(jsonPath("$.arquivos[0].nome").value("documento.pdf"));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Cenário 2: deve retornar 400 quando os idiomas de origem e destino são iguais")
    void deveRetornarBadRequestComIdiomasIguais() throws Exception {
        when(solicitacaoService.criar(any(), any()))
                .thenThrow(new SolicitacaoInvalidaException("Os idiomas de origem e destino não devem ser iguais"));

        mockMvc.perform(multipart(BASE_PATH)
                        .file(arquivoPdf())
                        .param("clienteId", clienteId.toString())
                        .param("categoriaProjetoId", categoriaId.toString())
                        .param("idiomaOrigemId", idiomaOrigemId.toString())
                        .param("idiomaDestinoId", idiomaOrigemId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Os idiomas de origem e destino não devem ser iguais"));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("RN.1: deve retornar 400 e não chamar o service quando falta o cliente")
    void deveRetornarBadRequestSemCliente() throws Exception {
        mockMvc.perform(multipart(BASE_PATH)
                        .file(arquivoPdf())
                        .param("categoriaProjetoId", categoriaId.toString())
                        .param("idiomaOrigemId", idiomaOrigemId.toString())
                        .param("idiomaDestinoId", idiomaDestinoId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cliente é obrigatório"))
                .andExpect(jsonPath("$.erros[0].campo").value("clienteId"));

        verify(solicitacaoService, never()).criar(any(), any());
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("RN.3: deve aceitar solicitação sem nenhum arquivo anexado")
    void deveAceitarSolicitacaoSemArquivo() throws Exception {
        when(solicitacaoService.criar(any(), any())).thenReturn(respostaCriada(UUID.randomUUID()));

        mockMvc.perform(multipart(BASE_PATH)
                        .param("clienteId", clienteId.toString())
                        .param("categoriaProjetoId", categoriaId.toString())
                        .param("idiomaOrigemId", idiomaOrigemId.toString())
                        .param("idiomaDestinoId", idiomaDestinoId.toString()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "financeiro@stentio.com", roles = {"FINANCEIRO"})
    @DisplayName("Deve retornar 403 quando o perfil não pode registrar solicitações")
    void deveRetornarForbiddenParaPerfilSemPermissao() throws Exception {
        mockMvc.perform(multipart(BASE_PATH)
                        .param("clienteId", clienteId.toString())
                        .param("categoriaProjetoId", categoriaId.toString())
                        .param("idiomaOrigemId", idiomaOrigemId.toString())
                        .param("idiomaDestinoId", idiomaDestinoId.toString()))
                .andExpect(status().isForbidden());

        verify(solicitacaoService, never()).criar(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando a requisição não tem token")
    void deveRetornarUnauthorizedSemToken() throws Exception {
        mockMvc.perform(multipart(BASE_PATH)
                        .param("clienteId", clienteId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
