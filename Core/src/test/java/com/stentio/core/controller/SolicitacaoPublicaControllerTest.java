package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.SolicitacaoPublicaService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
