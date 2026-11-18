package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.RecursoFiltro;
import com.stentio.core.dto.RecursoRequest;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.service.RecursoService;
import com.stentio.core.service.TokenService;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-023 CA.2, CA.6 e CA.7 no nível HTTP.
 *
 * <p>Os testes de service provam as regras; aqui a pergunta é outra: o SecurityConfig
 * deixa cada papel chegar no endpoint certo, e o corpo/response continuam ligados.
 */
@WebMvcTest(controllers = RecursoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RecursoControllerTest {

    private static final String BASE_PATH = "/api/v1/recursos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecursoService recursoService;

    @MockitoBean
    private TokenService tokenService;

    private RecursoResponse resposta(UUID id, boolean verificado) {
        return new RecursoResponse(
                id,
                "Maria Tradutora",
                "maria@exemplo.com",
                "+55 11 99999-0000",
                true,
                verificado,
                List.of(),
                List.of());
    }

    @Test
    @WithMockUser(username = "maria@exemplo.com", roles = "RECURSO")
    @DisplayName("CA.2: o profissional cria o próprio perfil")
    void recursoCriaProprioPerfil() throws Exception {
        UUID id = UUID.randomUUID();
        UUID tipoId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        RecursoRequest request = new RecursoRequest(
                "Maria Tradutora",
                "maria@exemplo.com",
                "+55 11 99999-0000",
                List.of(tipoId),
                List.of(new RecursoRequest.PrecoRequest(
                        origemId, destinoId, UnidadeCobranca.PALAVRA, new BigDecimal("0.25"))));

        when(recursoService.autoCadastrar(any(RecursoRequest.class), any(UsuarioAutenticado.class)))
                .thenReturn(resposta(id, false));

        mockMvc.perform(post(BASE_PATH + "/auto-cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_PATH + "/" + id))
                .andExpect(jsonPath("$.verificado").value(false));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = "ATENDENTE")
    @DisplayName("CA.2: a equipe não usa o auto-cadastro do profissional")
    void atendenteNaoCriaPerfilDeRecurso() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/auto-cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(recursoService, never()).autoCadastrar(any(), any());
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = "GESTOR_PROJETO")
    @DisplayName("CA.6: o gestor verifica o recurso")
    void gestorVerificaRecurso() throws Exception {
        UUID id = UUID.randomUUID();
        when(recursoService.alterarVerificacao(id, true)).thenReturn(resposta(id, true));

        mockMvc.perform(patch(BASE_PATH + "/" + id + "/verificacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificado\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.verificado").value(true));
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = "ATENDENTE")
    @DisplayName("CA.6: quem não é gestor não verifica recurso")
    void atendenteNaoVerificaRecurso() throws Exception {
        mockMvc.perform(patch(BASE_PATH + "/" + UUID.randomUUID() + "/verificacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificado\":true}"))
                .andExpect(status().isForbidden());

        verify(recursoService, never()).alterarVerificacao(any(), anyBoolean());
    }

    @Test
    @WithMockUser(username = "gestor@stentio.com", roles = "GESTOR_PROJETO")
    @DisplayName("CA.6: a fila de verificação é filtrada por verificado=false")
    void gestorFiltraPendentesDeVerificacao() throws Exception {
        when(recursoService.listar(any(RecursoFiltro.class), any(Pageable.class)))
                .thenReturn(Page.<RecursoResponse>empty());

        mockMvc.perform(get(BASE_PATH).param("verificado", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<RecursoFiltro> filtro = ArgumentCaptor.forClass(RecursoFiltro.class);
        verify(recursoService).listar(filtro.capture(), any(Pageable.class));
        assertFalse(filtro.getValue().verificado());
    }

    @Test
    @WithMockUser(username = "maria@exemplo.com", roles = "RECURSO")
    @DisplayName("CA.7: o profissional não lista a base de recursos da equipe")
    void recursoNaoListaBaseDaEquipe() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isForbidden());

        verify(recursoService, never()).listar(any(), any());
    }
}
