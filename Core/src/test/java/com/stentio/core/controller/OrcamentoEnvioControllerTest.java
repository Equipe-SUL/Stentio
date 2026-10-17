package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.OrcamentoAprovacaoResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.exception.OrcamentoIncompletoException;
import com.stentio.core.service.OrcamentoEnvioService;
import com.stentio.core.service.TokenService;
import com.stentio.core.model.StatusOrcamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrcamentoEnvioController.class,
        properties = "app.cors.allowed-origins=http://localhost:8080")
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrcamentoEnvioControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrcamentoEnvioService service;
    @MockitoBean TokenService tokenService;

    @Test
    void endpointRetornaContratoDeEnvioParaAtendente() throws Exception {
        UUID id = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        when(service.enviarParaAprovacao(id)).thenReturn(new OrcamentoAprovacaoResponse(
                id, StatusOrcamento.AGUARDANDO_APROVACAO, token,
                Instant.parse("2026-10-15T23:59:59Z"),
                "Orçamento enviado para a fila de e-mails com sucesso"));

        mvc.perform(post("/api/v1/orcamentos/{id}/enviar-aprovacao", id)
                        .with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.token_aprovacao").value(token.toString()))
                .andExpect(jsonPath("$.data_expiracao").value("2026-10-15T23:59:59Z"))
                .andExpect(jsonPath("$.mensagem").value("Orçamento enviado para a fila de e-mails com sucesso"));
    }

    @Test
    void endpointRejeitaSemAutenticacaoEPerfilNaoAutorizado() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/orcamentos/{id}/enviar-aprovacao", id))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/orcamentos/{id}/enviar-aprovacao", id)
                        .with(user("gestor").roles("GESTOR_PROJETO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void orcamentoSemValoresRetorna400() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.enviarParaAprovacao(id)).thenThrow(new OrcamentoIncompletoException());

        mvc.perform(post("/api/v1/orcamentos/{id}/enviar-aprovacao", id)
                        .with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(OrcamentoIncompletoException.MENSAGEM));
    }
}
