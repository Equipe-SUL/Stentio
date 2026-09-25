package com.stentio.core.service;

import com.stentio.core.dto.ClienteFiltro;
import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.exception.ClienteInvalidoException;
import com.stentio.core.exception.ClienteNaoEncontradoException;
import com.stentio.core.exception.CpfCnpjJaCadastradoException;
import com.stentio.core.mapper.ClienteMapper;
import com.stentio.core.model.Cliente;
import com.stentio.core.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Spy
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    private UUID clienteId;
    private UUID empresaId;
    private ClienteRequest requestValido;
    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        empresaId = UUID.randomUUID();

        requestValido = new ClienteRequest(
                "Acme Ltda",
                "Carlos Silva",
                "carlos@acme.com",
                "11987654321",
                empresaId,
                "52998224725"
        );

        clienteSalvo = new Cliente(
                requestValido.nomeEmpresa(),
                requestValido.nomeRepresentante(),
                requestValido.emailRepresentante(),
                requestValido.telefone(),
                requestValido.empresaId(),
                requestValido.cpfCnpj()
        );
    }

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso quando dados válidos")
    void deveCadastrarClienteComSucesso() {
        when(clienteRepository.existsByCpfCnpj("52998224725")).thenReturn(false);
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenReturn(clienteSalvo);

        ClienteResponse response = clienteService.criar(requestValido);

        assertNotNull(response);
        assertEquals("Acme Ltda", response.nomeEmpresa());
        assertEquals("Carlos Silva", response.nomeRepresentante());
        assertEquals("carlos@acme.com", response.emailRepresentante());
        assertEquals(empresaId, response.empresaId());
        assertEquals("52998224725", response.cpfCnpj());
        verify(clienteRepository).saveAndFlush(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar CpfCnpjJaCadastradoException ao tentar cadastrar cliente com CPF/CNPJ existente")
    void deveLancarExcecaoQuandoCpfCnpjJaCadastradoNoCadastro() {
        when(clienteRepository.existsByCpfCnpj("52998224725")).thenReturn(true);

        assertThrows(CpfCnpjJaCadastradoException.class, () -> clienteService.criar(requestValido));
        verify(clienteRepository, never()).saveAndFlush(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve editar cliente com sucesso")
    void deveEditarClienteComSucesso() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteSalvo));
        when(clienteRepository.existsByCpfCnpjAndIdNot(eq("52998224725"), any())).thenReturn(false);

        ClienteRequest requestEdicao = new ClienteRequest(
                "Acme Corp Atualizada",
                "Carlos Silva Modificado",
                "novo_email@acme.com",
                "11999999999",
                empresaId,
                "52998224725"
        );

        ClienteResponse response = clienteService.editar(clienteId, requestEdicao);

        assertNotNull(response);
        assertEquals("Acme Corp Atualizada", response.nomeEmpresa());
        assertEquals("Carlos Silva Modificado", response.nomeRepresentante());
        assertEquals("novo_email@acme.com", response.emailRepresentante());
        verify(clienteRepository).flush();
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao tentar editar cliente inexistente")
    void deveLancarExcecaoAoEditarClienteInexistente() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.editar(clienteId, requestValido));
    }

    @Test
    @DisplayName("Deve lançar CpfCnpjJaCadastradoException ao editar cliente para CPF/CNPJ pertencente a outro cliente")
    void deveLancarExcecaoAoEditarComCpfCnpjJaExistenteEmOutroCliente() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteSalvo));
        when(clienteRepository.existsByCpfCnpjAndIdNot(eq("52998224725"), any())).thenReturn(true);

        assertThrows(CpfCnpjJaCadastradoException.class, () -> clienteService.editar(clienteId, requestValido));
    }

    @Test
    @DisplayName("Deve excluir cliente com sucesso")
    void deveExcluirClienteComSucesso() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteSalvo));

        assertDoesNotThrow(() -> clienteService.excluir(clienteId));
        verify(clienteRepository).delete(clienteSalvo);
        verify(clienteRepository).flush();
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao tentar excluir cliente inexistente")
    void deveLancarExcecaoAoExcluirClienteInexistente() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.excluir(clienteId));
        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteSalvo));

        ClienteResponse response = clienteService.buscarPorId(clienteId);

        assertNotNull(response);
        assertEquals("Acme Ltda", response.nomeEmpresa());
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao buscar por ID inexistente")
    void deveLancarExcecaoAoBuscarPorIdInexistente() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.buscarPorId(clienteId));
    }

    @Test
    @DisplayName("Deve buscar cliente por CPF ou CNPJ com sucesso")
    void deveBuscarPorCpfCnpjComSucesso() {
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(clienteSalvo));

        ClienteResponse response = clienteService.buscarPorCpfCnpj("529.982.247-25");

        assertNotNull(response);
        assertEquals("Acme Ltda", response.nomeEmpresa());
        assertEquals("52998224725", response.cpfCnpj());
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao buscar por CPF/CNPJ inexistente")
    void deveLancarExcecaoAoBuscarPorCpfCnpjInexistente() {
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.buscarPorCpfCnpj("52998224725"));
    }

    @Test
    @DisplayName("Deve lançar ClienteInvalidoException ao buscar por CPF/CNPJ vazio")
    void deveLancarExcecaoAoBuscarPorCpfCnpjVazio() {
        assertThrows(ClienteInvalidoException.class, () -> clienteService.buscarPorCpfCnpj("   "));
    }

    @Test
    @DisplayName("Deve listar clientes por empresa com paginação")
    void deveListarClientesPorEmpresa() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(clienteSalvo), pageable, 1);
        when(clienteRepository.findAllByEmpresaId(empresaId, pageable)).thenReturn(page);

        Page<ClienteResponse> resultado = clienteService.listarPorEmpresa(empresaId, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Acme Ltda", resultado.getContent().get(0).nomeEmpresa());
    }

    @Test
    @DisplayName("Deve listar clientes com filtros e paginação")
    void deveListarClientesComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(clienteSalvo), pageable, 1);
        ClienteFiltro filtro = new ClienteFiltro("Acme", empresaId, null, null);

        when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), eq(pageable))).thenReturn(page);

        Page<ClienteResponse> resultado = clienteService.listar(filtro, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Acme Ltda", resultado.getContent().get(0).nomeEmpresa());
    }
}
