package com.stentio.core.service;

import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.exception.CpfCnpjJaCadastradoException;
import com.stentio.core.mapper.ClienteMapper;
import com.stentio.core.model.Cliente;
import com.stentio.core.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientePublicoServiceTest {

    private static final String CPF_FORMATADO = "529.982.247-25";
    private static final String CPF_NORMALIZADO = "52998224725";

    private ClienteRepository clienteRepository;
    private ClienteService clienteService;
    private ClientePublicoService service;
    private SolicitacaoPublicaRequest requisicao;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        clienteService = mock(ClienteService.class);
        service = new ClientePublicoService(clienteRepository, clienteService, new ClienteMapper());
        requisicao = new SolicitacaoPublicaRequest(
                "Empresa do formulário",
                "Representante do formulário",
                "novo@exemplo.com",
                "11999990000",
                CPF_FORMATADO,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Solicitação pública");
    }

    @Test
    void clienteExistenteEReutilizadoSemSobrescreverDados() {
        Cliente existente = new Cliente(
                "Empresa cadastrada", "Representante cadastrado", "antigo@exemplo.com",
                "11333334444", CPF_NORMALIZADO);
        when(clienteRepository.findByCpfCnpj(CPF_NORMALIZADO)).thenReturn(Optional.of(existente));

        ClienteResponse resposta = service.buscarOuCriar(requisicao);

        assertEquals("Empresa cadastrada", resposta.nomeEmpresa());
        assertEquals("Representante cadastrado", resposta.nomeRepresentante());
        assertEquals("antigo@exemplo.com", resposta.emailRepresentante());
        assertEquals("11333334444", resposta.telefone());
        verify(clienteService, never()).criar(any(ClienteRequest.class));
    }

    @Test
    void clienteNovoECriadoComDocumentoNormalizado() {
        when(clienteRepository.findByCpfCnpj(CPF_NORMALIZADO)).thenReturn(Optional.empty());
        UUID novoId = UUID.randomUUID();
        ClienteResponse criado = new ClienteResponse(
                novoId, "Empresa do formulário", "Representante do formulário",
                "novo@exemplo.com", "11999990000", CPF_NORMALIZADO, null, null);
        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(criado);

        ClienteResponse resposta = service.buscarOuCriar(requisicao);

        assertEquals(novoId, resposta.id());
        assertEquals(CPF_NORMALIZADO, resposta.cpfCnpj());
        verify(clienteService).criar(new ClienteRequest(
                "Empresa do formulário",
                "Representante do formulário",
                "novo@exemplo.com",
                "11999990000",
                CPF_NORMALIZADO));
    }

    @Test
    void resolveConflitoDeUnicidadeBuscandoClienteCriadoConcorrentemente() {
        Cliente concorrente = new Cliente(
                "Empresa vencedora", "Representante vencedor", "vencedor@exemplo.com",
                null, CPF_NORMALIZADO);
        when(clienteRepository.findByCpfCnpj(CPF_NORMALIZADO))
                .thenReturn(Optional.empty(), Optional.of(concorrente));
        when(clienteService.criar(any(ClienteRequest.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        ClienteResponse resposta = service.buscarOuCriar(requisicao);

        assertEquals("Empresa vencedora", resposta.nomeEmpresa());
        verify(clienteRepository, times(2)).findByCpfCnpj(CPF_NORMALIZADO);
    }

    @Test
    void resolveDuplicidadeDetectadaAntesDoInsert() {
        Cliente concorrente = new Cliente(
                "Empresa vencedora", "Representante vencedor", "vencedor@exemplo.com",
                null, CPF_NORMALIZADO);
        when(clienteRepository.findByCpfCnpj(CPF_NORMALIZADO))
                .thenReturn(Optional.empty(), Optional.of(concorrente));
        when(clienteService.criar(any(ClienteRequest.class)))
                .thenThrow(new CpfCnpjJaCadastradoException());

        ClienteResponse resposta = service.buscarOuCriar(requisicao);

        assertEquals("Empresa vencedora", resposta.nomeEmpresa());
    }

    @Test
    void propagaViolacaoNaoRelacionadaQuandoNaoEncontraClienteConcorrente() {
        DataIntegrityViolationException falha = new DataIntegrityViolationException("outra constraint");
        when(clienteRepository.findByCpfCnpj(CPF_NORMALIZADO))
                .thenReturn(Optional.empty(), Optional.empty());
        when(clienteService.criar(any(ClienteRequest.class))).thenThrow(falha);

        DataIntegrityViolationException erro = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.buscarOuCriar(requisicao));

        assertSame(falha, erro);
    }
}
