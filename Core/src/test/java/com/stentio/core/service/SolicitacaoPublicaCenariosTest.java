package com.stentio.core.service;

import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.dto.SolicitacaoPublicaResponse;
import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.mapper.ClienteMapper;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitacaoPublicaCenariosTest {

    @Test
    void clienteNovoEAssociadoASolicitacaoPendenteDeOrcamento() {
        UUID clienteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        UUID solicitacaoId = UUID.randomUUID();
        ClienteRepository clientes = mock(ClienteRepository.class);
        ClienteService cadastroClientes = mock(ClienteService.class);
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoService solicitacoes = mock(SolicitacaoService.class);
        SolicitacaoPublicaService service = service(clientes, cadastroClientes, categorias, idiomas, solicitacoes);
        SolicitacaoPublicaRequest requisicao = requisicao(categoriaId, idiomaOrigemId, idiomaDestinoId);

        prepararReferencias(categorias, idiomas, categoriaId, idiomaOrigemId, idiomaDestinoId);
        when(clientes.findByCpfCnpj("52998224725")).thenReturn(Optional.empty());
        when(cadastroClientes.criar(any(ClienteRequest.class))).thenReturn(clienteResponse(clienteId));
        when(solicitacoes.criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                List.of()))
                .thenReturn(solicitacaoResponse(solicitacaoId));

        SolicitacaoPublicaResponse resposta = service.criar(requisicao, List.of());

        assertEquals(solicitacaoId, resposta.id());
        assertEquals(StatusSolicitacao.PENDENTE_ORCAMENTO, resposta.status());
        verify(cadastroClientes).criar(new ClienteRequest(
                "Empresa Exemplo", "Representante", "cliente@exemplo.com", null, "52998224725"));
        verify(solicitacoes).criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                List.of());
    }

    @Test
    void clienteExistenteEAssociadoSemAtualizarNemDuplicarCadastro() {
        UUID clienteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        UUID solicitacaoId = UUID.randomUUID();
        ClienteRepository clientes = mock(ClienteRepository.class);
        ClienteService cadastroClientes = mock(ClienteService.class);
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoService solicitacoes = mock(SolicitacaoService.class);
        SolicitacaoPublicaService service = service(clientes, cadastroClientes, categorias, idiomas, solicitacoes);
        SolicitacaoPublicaRequest requisicao = requisicao(categoriaId, idiomaOrigemId, idiomaDestinoId);
        Cliente existente = new Cliente(
                "Empresa já cadastrada", "Representante original", "original@exemplo.com",
                "11911112222", "52998224725");
        ReflectionTestUtils.setField(existente, "id", clienteId);

        prepararReferencias(categorias, idiomas, categoriaId, idiomaOrigemId, idiomaDestinoId);
        when(clientes.findByCpfCnpj("52998224725")).thenReturn(Optional.of(existente));
        when(solicitacoes.criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                List.of()))
                .thenReturn(solicitacaoResponse(solicitacaoId));

        SolicitacaoPublicaResponse resposta = service.criar(requisicao, List.of());

        assertEquals(solicitacaoId, resposta.id());
        assertEquals(StatusSolicitacao.PENDENTE_ORCAMENTO, resposta.status());
        verify(cadastroClientes, never()).criar(any(ClienteRequest.class));
        verify(solicitacoes).criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                List.of());
        assertEquals("Empresa já cadastrada", existente.getNomeEmpresa());
        assertEquals("original@exemplo.com", existente.getEmailRepresentante());
    }

    private static SolicitacaoPublicaService service(
            ClienteRepository clientes,
            ClienteService cadastroClientes,
            CategoriaProjetoRepository categorias,
            IdiomaRepository idiomas,
            SolicitacaoService solicitacoes) {
        ClientePublicoService clientesPublicos =
                new ClientePublicoService(clientes, cadastroClientes, new ClienteMapper());
        return new SolicitacaoPublicaService(categorias, idiomas, clientesPublicos, solicitacoes);
    }

    private static SolicitacaoPublicaRequest requisicao(UUID categoriaId, UUID origemId, UUID destinoId) {
        return new SolicitacaoPublicaRequest(
                "Empresa Exemplo", "Representante", "cliente@exemplo.com", null,
                "529.982.247-25", categoriaId, origemId, destinoId, "Traduzir contrato");
    }

    private static ClienteResponse clienteResponse(UUID id) {
        return new ClienteResponse(
                id, "Empresa Exemplo", "Representante", "cliente@exemplo.com",
                null, "52998224725", null, null);
    }

    private static SolicitacaoResponse solicitacaoResponse(UUID id) {
        return new SolicitacaoResponse(
                id, null, null, null, null, null,
                StatusSolicitacao.PENDENTE_ORCAMENTO, null, List.of());
    }

    private static void prepararReferencias(
            CategoriaProjetoRepository categorias,
            IdiomaRepository idiomas,
            UUID categoriaId,
            UUID origemId,
            UUID destinoId) {
        when(categorias.existsById(categoriaId)).thenReturn(true);
        Idioma origem = new Idioma();
        origem.setId(origemId);
        origem.setAtivo(true);
        Idioma destino = new Idioma();
        destino.setId(destinoId);
        destino.setAtivo(true);
        when(idiomas.findById(origemId)).thenReturn(Optional.of(origem));
        when(idiomas.findById(destinoId)).thenReturn(Optional.of(destino));
    }
}
