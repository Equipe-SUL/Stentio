package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.AprovacaoEntregaFinalResponse;
import com.stentio.core.dto.ReprovacaoEntregaFinalResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.service.AprovacaoEntregaFinalService;
import com.stentio.core.service.EntregaFinalService;
import com.stentio.core.service.ReprovacaoEntregaFinalService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-015 CA.1: a entrega final é decisão de quem gerencia o projeto. */
@WebMvcTest(controllers = EntregaFinalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EntregaFinalControllerTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final String BASE = "/api/v1/ordens-servico/" + OS_ID + "/entrega-final";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaFinalService entregaFinalService;

    @MockitoBean
    private AprovacaoEntregaFinalService aprovacaoEntregaFinalService;

    @MockitoBean
    private ReprovacaoEntregaFinalService reprovacaoEntregaFinalService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("o gestor abre a revisão")
    void gestorRevisa() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isOk());

        verify(entregaFinalService).revisar(OS_ID);
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("CA.7: aprovar responde 202, porque a OS ainda não fechou")
    void aprovarResponde202() throws Exception {
        when(aprovacaoEntregaFinalService.aprovar(any(), any(), any())).thenReturn(
                new AprovacaoEntregaFinalResponse(
                        UUID.randomUUID(), "ENTREGA_FINAL:x", StatusOrdemServico.AGUARDANDO_VERIFICACAO));

        mockMvc.perform(post(BASE + "/aprovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagem\":\"Segue o documento final.\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_VERIFICACAO"))
                .andExpect(jsonPath("$.idempotency_key").value("ENTREGA_FINAL:x"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("aprovar sem corpo também vale: a mensagem ao cliente é opcional")
    void aprovarSemCorpo() throws Exception {
        when(aprovacaoEntregaFinalService.aprovar(any(), any(), any())).thenReturn(
                new AprovacaoEntregaFinalResponse(
                        UUID.randomUUID(), "ENTREGA_FINAL:x", StatusOrdemServico.AGUARDANDO_VERIFICACAO));

        mockMvc.perform(post(BASE + "/aprovar")).andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("CA.10: reprovar sem motivo é recusado antes de chegar ao serviço")
    void reprovarExigeMotivo() throws Exception {
        mockMvc.perform(post(BASE + "/reprovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(reprovacaoEntregaFinalService, never()).reprovar(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTOR_PROJETO")
    @DisplayName("reprovar devolve onde a etapa e a OS ficaram")
    void reprovarResponde200() throws Exception {
        when(reprovacaoEntregaFinalService.reprovar(any(), any(), any())).thenReturn(
                new ReprovacaoEntregaFinalResponse(
                        UUID.randomUUID(), StatusEtapaOrdemServico.ACEITA, StatusOrdemServico.EM_EXECUCAO));

        mockMvc.perform(post(BASE + "/reprovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Falta a assinatura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa_status").value("ACEITA"))
                .andExpect(jsonPath("$.ordem_servico_status").value("EM_EXECUCAO"));
    }

    @Test
    @WithMockUser(roles = "RECURSO")
    @DisplayName("o profissional não revisa nem aprova a própria entrega")
    void recursoNaoAcessa() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/aprovar")).andExpect(status().isForbidden());

        verify(aprovacaoEntregaFinalService, never()).aprovar(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("CA.1: atendente não aprova entrega final")
    void atendenteNaoAprova() throws Exception {
        mockMvc.perform(post(BASE + "/aprovar")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sem autenticação não passa")
    void anonimoNaoAcessa() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }
}
