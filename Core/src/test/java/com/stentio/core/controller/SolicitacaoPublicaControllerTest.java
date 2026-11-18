package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.dto.SolicitacaoPublicaResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.service.SolicitacaoPublicaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SolicitacaoPublicaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SolicitacaoPublicaControllerTest {

    private static final String BASE_PATH = "/api/v1/public/solicitacoes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitacaoPublicaService service;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void deveRetornarOpcoesSemAutenticacaoSomenteComCamposPublicos() throws Exception {
        UUID categoriaId = UUID.randomUUID();
        UUID idiomaId = UUID.randomUUID();
        when(service.obterOpcoes()).thenReturn(new SolicitacaoOpcoesResponse(
                List.of(new SolicitacaoOpcoesResponse.CategoriaOpcao(categoriaId, "Contrato")),
                List.of(new SolicitacaoOpcoesResponse.IdiomaOpcao(idiomaId, "Português", "pt"))
        ));

        mockMvc.perform(get(BASE_PATH + "/opcoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriasProjeto.length()").value(1))
                .andExpect(jsonPath("$.categoriasProjeto[0].id").value(categoriaId.toString()))
                .andExpect(jsonPath("$.categoriasProjeto[0].nome").value("Contrato"))
                .andExpect(jsonPath("$.categoriasProjeto[0].ativo").doesNotExist())
                .andExpect(jsonPath("$.idiomas.length()").value(1))
                .andExpect(jsonPath("$.idiomas[0].id").value(idiomaId.toString()))
                .andExpect(jsonPath("$.idiomas[0].nome").value("Português"))
                .andExpect(jsonPath("$.idiomas[0].codigoIso").value("pt"))
                .andExpect(jsonPath("$.idiomas[0].ativo").doesNotExist());
    }

    @Test
    void deveCriarSolicitacaoMultiparteSemAutenticacaoERetornarSomenteIdEStatus() throws Exception {
        UUID solicitacaoId = UUID.randomUUID();
        when(service.criar(any(), anyList())).thenReturn(
                new SolicitacaoPublicaResponse(solicitacaoId, StatusSolicitacao.PENDENTE_ORCAMENTO));

        mockMvc.perform(multipart(BASE_PATH)
                        .file(new MockMultipartFile("arquivos", "contrato.pdf", "application/pdf", new byte[]{1}))
                        .file(new MockMultipartFile("arquivos", "anexo.pdf", "application/pdf", new byte[]{2}))
                        .param("nomeEmpresa", "Empresa Exemplo")
                        .param("nomeRepresentante", "Pessoa Representante")
                        .param("emailRepresentante", "cliente@exemplo.com")
                        .param("telefone", "11999990000")
                        .param("cpfCnpj", "52998224725")
                        .param("categoriaProjetoId", UUID.randomUUID().toString())
                        .param("idiomaOrigemId", UUID.randomUUID().toString())
                        .param("idiomaDestinoId", UUID.randomUUID().toString())
                        .param("descricao", "Tradução de contrato"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE_ORCAMENTO"))
                .andExpect(jsonPath("$.cliente").doesNotExist())
                .andExpect(jsonPath("$.categoriaProjeto").doesNotExist())
                .andExpect(jsonPath("$.descricao").doesNotExist())
                .andExpect(jsonPath("$.arquivos").doesNotExist());

        verify(service).criar(any(), argThat(arquivos -> arquivos != null && arquivos.size() == 2));
    }

    @Test
    void deveInformarCamposObrigatoriosInvalidosSemChamarService() throws Exception {
        mockMvc.perform(multipart(BASE_PATH)
                        .param("nomeEmpresa", "Empresa Exemplo")
                        .param("nomeRepresentante", "Pessoa Representante")
                        .param("emailRepresentante", "cliente@exemplo.com")
                        .param("cpfCnpj", "52998224725"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder(
                        "categoriaProjetoId", "idiomaOrigemId", "idiomaDestinoId")));

        verify(service, never()).criar(any(), anyList());
    }

    @Test
    void deveAceitarCriacaoSemAnexos() throws Exception {
        UUID solicitacaoId = UUID.randomUUID();
        when(service.criar(any(), any())).thenReturn(
                new SolicitacaoPublicaResponse(solicitacaoId, StatusSolicitacao.PENDENTE_ORCAMENTO));

        mockMvc.perform(multipart(BASE_PATH)
                        .param("nomeEmpresa", "Empresa Exemplo")
                        .param("nomeRepresentante", "Pessoa Representante")
                        .param("emailRepresentante", "cliente@exemplo.com")
                        .param("cpfCnpj", "52998224725")
                        .param("categoriaProjetoId", UUID.randomUUID().toString())
                        .param("idiomaOrigemId", UUID.randomUUID().toString())
                        .param("idiomaDestinoId", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()));

        verify(service).criar(any(), any());
    }

    @Test
    void deveRetornarBadRequestQuandoIdiomasForemIguais() throws Exception {
        UUID idiomaId = UUID.randomUUID();

        mockMvc.perform(multipart(BASE_PATH)
                        .param("nomeEmpresa", "Empresa Exemplo")
                        .param("nomeRepresentante", "Pessoa Representante")
                        .param("emailRepresentante", "cliente@exemplo.com")
                        .param("cpfCnpj", "52998224725")
                        .param("categoriaProjetoId", UUID.randomUUID().toString())
                        .param("idiomaOrigemId", idiomaId.toString())
                        .param("idiomaDestinoId", idiomaId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("idiomaDestinoId"));

        verify(service, never()).criar(any(), anyList());
    }

    @Test
    void rotasPublicasNaoExpostasExigemAutenticacao() throws Exception {
        String solicitacaoId = UUID.randomUUID().toString();

        mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_PATH + "/" + solicitacaoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE_PATH + "/" + solicitacaoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE_PATH + "/" + solicitacaoId)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE_PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE_PATH + "/" + solicitacaoId)).andExpect(status().isUnauthorized());
    }
}
