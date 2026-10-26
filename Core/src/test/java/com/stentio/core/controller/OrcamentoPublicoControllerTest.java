package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.*;
import com.stentio.core.exception.*;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.service.OrcamentoPublicoService;
import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrcamentoPublicoController.class, properties = "app.cors.allowed-origins=http://localhost:8080")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OrcamentoPublicoExceptionHandler.class})
class OrcamentoPublicoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrcamentoPublicoService service;
    @MockitoBean ArquivoStorageService arquivoStorageService;
    @MockitoBean TokenService tokenService;
    private final String token = "6bbd9908-4783-4991-a4f9-b0fc1807115a";
    private final String path = "/api/v1/public/orcamentos";

    @Test
    void consultaSemLoginPreservaContrato() throws Exception {
        var item = new OrcamentoPublicoResponse.Item("Tradução Juramentada", new BigDecimal("5000"),
                "Palavras", new BigDecimal("0.30"), new BigDecimal("1500.00"));
        when(service.consultar(token)).thenReturn(new OrcamentoPublicoResponse(UUID.randomUUID(),
                StatusOrcamento.AGUARDANDO_APROVACAO, new BigDecimal("1500.00"),
                Instant.parse("2026-10-15T23:59:59Z"), List.of(item), 1,
                Instant.parse("2026-10-10T17:00:00Z"), null));
        mvc.perform(get(path).param("token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.valor_total").value(1500.00))
                .andExpect(jsonPath("$.data_expiracao").value("2026-10-15T23:59:59Z"))
                .andExpect(jsonPath("$.itens[0]['tipo_de_serviço']").value("Tradução Juramentada"))
                .andExpect(jsonPath("$.itens[0].valor_unitario").value(0.30))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.cliente").doesNotExist());
    }

    @Test
    void aprovaSemLogin() throws Exception {
        when(service.aprovar(token)).thenReturn(new OrcamentoDecisaoResponse(UUID.randomUUID(),
                StatusOrcamento.APROVADO, Instant.parse("2026-10-10T17:00:00Z")));
        mvc.perform(post(path + "/aprovar").contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void rejeitaSemLogin() throws Exception {
        when(service.rejeitar(token, "Preço alto")).thenReturn(new OrcamentoDecisaoResponse(UUID.randomUUID(),
                StatusOrcamento.REJEITADO, Instant.parse("2026-10-10T17:00:00Z")));
        mvc.perform(post(path + "/rejeitar").contentType("application/json")
                        .content("{\"token\":\"" + token + "\",\"motivo\":\"Preço alto\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADO"));
    }

    @Test
    void tokenExpiradoRetorna400ComMensagemGenerica() throws Exception {
        when(service.aprovar(token)).thenThrow(new LinkOrcamentoInvalidoException());
        mvc.perform(post(path + "/aprovar").contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(LinkOrcamentoInvalidoException.MENSAGEM))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void consultaSemTokenRetorna400() throws Exception {
        when(service.consultar(isNull())).thenThrow(new LinkOrcamentoInvalidoException());
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }

    @Test
    void tokenInexistenteUsadoOuMalformadoRetorna400NoGet() throws Exception {
        when(service.consultar(anyString())).thenThrow(new LinkOrcamentoInvalidoException());
        for (String valor : List.of(token, "invalid", "")) {
            mvc.perform(get(path).param("token", valor)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void tokenAusenteNoPostRetorna400SemChamarService() throws Exception {
        mvc.perform(post(path + "/aprovar").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejeicaoExigeMotivoELimitaTamanho() throws Exception {
        for (String motivo : List.of("", " ", "a".repeat(2001))) {
            mvc.perform(post(path + "/rejeitar").contentType("application/json")
                            .content("{\"token\":\"" + token + "\",\"motivo\":\"" + motivo + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void jsonMalformadoRetorna400SemExporValorRecebido() throws Exception {
        mvc.perform(post(path + "/aprovar").contentType("application/json").content("{\"token\":{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Corpo da requisição inválido"));
    }

    @Test
    void outrasRotasPermanecemAutenticadas() throws Exception {
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(post(path + "/outro")).andExpect(status().isUnauthorized());
        mvc.perform(delete(path)).andExpect(status().isUnauthorized());
    }
}
