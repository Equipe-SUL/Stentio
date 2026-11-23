package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.CertificacaoPaginaResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.CertificacaoAnaliseService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-024 CA.6: a fila expõe documentos pessoais, e nem toda a equipe tem o que fazer com
 * eles. É o caso que justifica o matcher próprio, antes da regra geral de /recursos — sem
 * ele, ATENDENTE e FINANCEIRO passariam no GET como passam no resto do cadastro.
 */
@WebMvcTest(controllers = CertificacaoAnaliseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CertificacaoAnaliseControllerTest {

    private static final String BASE = "/api/v1/recursos/certificacoes";
    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificacaoAnaliseService certificacaoAnaliseService;

    @MockitoBean
    private ArquivoStorageService arquivoStorageService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("o gestor vê a fila")
    void gestorVeAFila() throws Exception {
        when(certificacaoAnaliseService.listarParaAnalise(any(), anyInt(), anyInt()))
                .thenReturn(new CertificacaoPaginaResponse(0, 1, 20, 0, List.of()));

        mockMvc.perform(get(BASE)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("o admin também")
    void adminVeAFila() throws Exception {
        when(certificacaoAnaliseService.listarParaAnalise(any(), anyInt(), anyInt()))
                .thenReturn(new CertificacaoPaginaResponse(0, 1, 20, 0, List.of()));

        mockMvc.perform(get(BASE)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Cenário 5: ATENDENTE não vê a fila")
    void atendenteNaoVeAFila() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());

        verify(certificacaoAnaliseService, never()).listarParaAnalise(any(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(roles = "FINANCEIRO")
    @DisplayName("CA.6: FINANCEIRO também não")
    void financeiroNaoVeAFila() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("RN.8: nem baixa o documento de ninguém")
    void atendenteNaoBaixaDocumento() throws Exception {
        mockMvc.perform(get(BASE + "/" + ID + "/arquivo")).andExpect(status().isForbidden());

        verify(certificacaoAnaliseService, never()).localizarDocumento(any());
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("o profissional não analisa certificação, nem a própria")
    void recursoNaoAnalisa() throws Exception {
        mockMvc.perform(post(BASE + "/" + ID + "/aprovar")).andExpect(status().isForbidden());

        verify(certificacaoAnaliseService, never()).aprovar(any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("CA.2: rejeitar sem motivo é recusado antes de chegar ao serviço")
    void rejeitarExigeMotivo() throws Exception {
        mockMvc.perform(post(BASE + "/" + ID + "/rejeitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(certificacaoAnaliseService, never()).rejeitar(any(), any(), any());
    }

    @Test
    @DisplayName("sem autenticação não passa")
    void anonimoNaoAcessa() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }
}
