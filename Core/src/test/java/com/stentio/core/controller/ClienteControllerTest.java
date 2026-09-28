package com.stentio.core.controller;

import com.stentio.core.config.SecurityConfig;
import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.exception.ClienteNaoEncontradoException;
import com.stentio.core.exception.CpfCnpjJaCadastradoException;
import com.stentio.core.exception.GlobalExceptionHandler;
import com.stentio.core.service.ClienteService;
import com.stentio.core.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClienteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private TokenService tokenService;

    private static final String BASE_PATH = "/api/v1/clientes";

    @Test
    @WithMockUser(username = "operador@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve cadastrar cliente com sucesso quando autenticado e dados válidos")
    void deveCadastrarClienteComSucesso() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        ClienteRequest request = new ClienteRequest(
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725"
        );

        ClienteResponse response = new ClienteResponse(
                clienteId,
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(clienteId.toString()))
                .andExpect(jsonPath("$.nomeEmpresa").value("Stentio Tecnologia"))
                .andExpect(jsonPath("$.nomeRepresentante").value("Wesley Xavier"))
                .andExpect(jsonPath("$.emailRepresentante").value("wesley@stentio.com"))
                .andExpect(jsonPath("$.cpfCnpj").value("52998224725"));
    }

    @Test
    @WithMockUser(username = "operador@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve retornar 400 Bad Request ao tentar cadastrar cliente com e-mail inválido")
    void deveRetornarBadRequestComEmailInvalido() throws Exception {
        UUID empresaId = UUID.randomUUID();

        ClienteRequest request = new ClienteRequest(
                "Stentio Tecnologia",
                "Wesley Xavier",
                "email_invalido_sem_arroba",
                "11987654321",
                empresaId,
                "52998224725"
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operador@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve retornar 400 Bad Request ao tentar cadastrar cliente com CPF/CNPJ inválido")
    void deveRetornarBadRequestComCpfInvalido() throws Exception {
        UUID empresaId = UUID.randomUUID();

        ClienteRequest request = new ClienteRequest(
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "11111111111" // dígitos repetidos
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operador@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve retornar 409 Conflict ao tentar cadastrar cliente com CPF/CNPJ já existente")
    void deveRetornarConflictComCpfDuplicado() throws Exception {
        UUID empresaId = UUID.randomUUID();

        ClienteRequest request = new ClienteRequest(
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725"
        );

        when(clienteService.criar(any(ClienteRequest.class)))
                .thenThrow(new CpfCnpjJaCadastradoException());

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(CpfCnpjJaCadastradoException.MENSAGEM));
    }

    @Test
    @WithMockUser(username = "usuario@stentio.com", roles = {"FINANCEIRO"})
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void deveBuscarPorIdComSucesso() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        ClienteResponse response = new ClienteResponse(
                clienteId,
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(clienteService.buscarPorId(clienteId)).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/" + clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteId.toString()))
                .andExpect(jsonPath("$.nomeEmpresa").value("Stentio Tecnologia"));
    }

    @Test
    @WithMockUser(username = "usuario@stentio.com", roles = {"FINANCEIRO"})
    @DisplayName("Deve retornar 404 Not Found ao buscar ID inexistente")
    void deveRetornarNotFoundAoBuscarIdInexistente() throws Exception {
        UUID clienteId = UUID.randomUUID();
        when(clienteService.buscarPorId(clienteId)).thenThrow(new ClienteNaoEncontradoException(clienteId));

        mockMvc.perform(get(BASE_PATH + "/" + clienteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "usuario@stentio.com", roles = {"FINANCEIRO"})
    @DisplayName("Deve buscar cliente por CPF/CNPJ com sucesso")
    void deveBuscarPorCpfCnpjComSucesso() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        ClienteResponse response = new ClienteResponse(
                clienteId,
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(clienteService.buscarPorCpfCnpj("52998224725")).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/cpf-cnpj/52998224725"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteId.toString()))
                .andExpect(jsonPath("$.cpfCnpj").value("52998224725"));
    }

    @Test
    @WithMockUser(username = "usuario@stentio.com", roles = {"GESTOR_PROJETO"})
    @DisplayName("Deve listar clientes por empresa com sucesso")
    void deveListarPorEmpresaComSucesso() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        ClienteResponse response = new ClienteResponse(
                clienteId,
                "Stentio Tecnologia",
                "Wesley Xavier",
                "wesley@stentio.com",
                "11987654321",
                empresaId,
                "52998224725",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(clienteService.listarPorEmpresa(eq(empresaId), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get(BASE_PATH + "/empresa/" + empresaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clienteId.toString()));
    }

    @Test
    @WithMockUser(username = "usuario@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve modificar cliente com sucesso quando autenticado")
    void deveModificarClienteComSucesso() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        ClienteRequest request = new ClienteRequest(
                "Stentio Tecnologia Atualizada",
                "Wesley Xavier Modificado",
                "wesley.novo@stentio.com",
                "11911112222",
                empresaId,
                "52998224725"
        );

        ClienteResponse response = new ClienteResponse(
                clienteId,
                "Stentio Tecnologia Atualizada",
                "Wesley Xavier Modificado",
                "wesley.novo@stentio.com",
                "11911112222",
                empresaId,
                "52998224725",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(clienteService.editar(eq(clienteId), any(ClienteRequest.class))).thenReturn(response);

        mockMvc.perform(put(BASE_PATH + "/" + clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeEmpresa").value("Stentio Tecnologia Atualizada"))
                .andExpect(jsonPath("$.emailRepresentante").value("wesley.novo@stentio.com"));
    }

    @Test
    @WithMockUser(username = "admin@stentio.com", roles = {"ADMIN"})
    @DisplayName("Deve permitir exclusão de cliente quando usuário possui role mais alta (ADMIN)")
    void deveExcluirClienteComRoleAdmin() throws Exception {
        UUID clienteId = UUID.randomUUID();
        doNothing().when(clienteService).excluir(clienteId);

        mockMvc.perform(delete(BASE_PATH + "/" + clienteId))
                .andExpect(status().isNoContent());

        verify(clienteService).excluir(clienteId);
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Deve proibir (403 Forbidden) exclusão de cliente quando usuário NÃO é ADMIN")
    void deveNegarExclusaoParaRoleNaoAdmin() throws Exception {
        UUID clienteId = UUID.randomUUID();

        mockMvc.perform(delete(BASE_PATH + "/" + clienteId))
                .andExpect(status().isForbidden());

        verify(clienteService, never()).excluir(any());
    }

    @Test
    @DisplayName("Deve rejeitar (401 Unauthorized) qualquer requisição para clientes sem autenticação")
    void deveRejeitarAcessoSemAutenticacao() throws Exception {
        UUID clienteId = UUID.randomUUID();

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_PATH + "/" + clienteId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete(BASE_PATH + "/" + clienteId))
                .andExpect(status().isUnauthorized());
    }
}
