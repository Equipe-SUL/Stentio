package com.stentio.core.service;

import com.stentio.core.dto.OrdemServicoDetalheResponse;
import com.stentio.core.dto.OrdemServicoPaginaResponse;
import com.stentio.core.dto.OrdemServicoResumoResponse;
import com.stentio.core.exception.OrdemServicoInvalidaException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.mapper.OrdemServicoMapper;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.OrdemServicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoConsultaServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;

    @Mock
    private OrdemServicoMapper ordemServicoMapper;

    @InjectMocks
    private OrdemServicoConsultaService ordemServicoConsultaService;

    private final UUID ordemId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    private OrdemServicoResumoResponse resumo() {
        return new OrdemServicoResumoResponse(
                ordemId, "OS-2026-0001", StatusOrdemServico.PENDENTE_ALOCACAO, clienteId, "Empresa XPTO", Instant.now());
    }

    @Test
    @DisplayName("CA.1 e CA.2: deve consultar com os filtros e devolver o envelope paginado")
    void deveListarComFiltros() {
        OrdemServico ordem = Mockito.mock(OrdemServico.class);
        when(ordemServicoRepository.findAll(
                ArgumentMatchers.<Specification<OrdemServico>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ordem), PageRequest.of(2, 5), 11));
        when(ordemServicoMapper.paraResumo(ordem)).thenReturn(resumo());

        OrdemServicoPaginaResponse resposta = ordemServicoConsultaService
                .listar(StatusOrdemServico.PENDENTE_ALOCACAO, clienteId, 3, 5);

        // A página volta em base 1 para o front, como no módulo de orçamento.
        assertEquals(3, resposta.pagina());
        assertEquals(5, resposta.tamanhoPagina());
        assertEquals(11, resposta.totalElementos());
        assertEquals("Empresa XPTO", resposta.conteudo().getFirst().clienteNome());
    }

    @Test
    @DisplayName("Deve converter a página base 1 para a base 0 do Spring Data e ordenar por data")
    void deveConverterPaginaEOrdenar() {
        when(ordemServicoRepository.findAll(
                ArgumentMatchers.<Specification<OrdemServico>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ordemServicoConsultaService.listar(null, null, 4, 20);

        ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
        verify(ordemServicoRepository).findAll(
                ArgumentMatchers.<Specification<OrdemServico>>any(), paginacao.capture());

        assertEquals(3, paginacao.getValue().getPageNumber());
        assertEquals(20, paginacao.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "criadoEm"), paginacao.getValue().getSort());
    }

    @Test
    @DisplayName("Deve recusar página menor que 1")
    void deveRecusarPaginaInvalida() {
        OrdemServicoInvalidaException erro = assertThrows(OrdemServicoInvalidaException.class,
                () -> ordemServicoConsultaService.listar(null, null, 0, 10));

        assertEquals("A página deve ser maior ou igual a 1", erro.getMessage());
        verify(ordemServicoRepository, never()).findAll(
                ArgumentMatchers.<Specification<OrdemServico>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve recusar tamanho de página fora do intervalo permitido")
    void deveRecusarTamanhoInvalido() {
        assertThrows(OrdemServicoInvalidaException.class,
                () -> ordemServicoConsultaService.listar(null, null, 1, 0));
        // Sem teto, um tamanho enorme carregaria a tabela inteira em memória.
        assertThrows(OrdemServicoInvalidaException.class,
                () -> ordemServicoConsultaService.listar(null, null, 1, 101));

        verify(ordemServicoRepository, never()).findAll(
                ArgumentMatchers.<Specification<OrdemServico>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("CA.3: deve converter a OS encontrada para a resposta detalhada")
    void deveBuscarDetalhePorId() {
        OrdemServico ordem = Mockito.mock(OrdemServico.class);
        OrdemServicoDetalheResponse detalhe = new OrdemServicoDetalheResponse(
                ordemId,
                "OS-2026-0001",
                StatusOrdemServico.PENDENTE_ALOCACAO,
                UUID.randomUUID(),
                Instant.now(),
                new OrdemServicoDetalheResponse.Cliente(clienteId, "Empresa XPTO"),
                new OrdemServicoDetalheResponse.Solicitacao(
                        UUID.randomUUID(), "Contrato", "Jurídico", "Português", "Inglês", List.of()));

        when(ordemServicoRepository.findById(ordemId)).thenReturn(Optional.of(ordem));
        when(ordemServicoMapper.paraDetalhe(ordem)).thenReturn(detalhe);

        assertEquals(detalhe, ordemServicoConsultaService.buscarPorId(ordemId));
    }

    @Test
    @DisplayName("Deve lançar not found quando o id não existe")
    void deveLancarQuandoNaoEncontra() {
        when(ordemServicoRepository.findById(ordemId)).thenReturn(Optional.empty());

        OrdemServicoNaoEncontradaException erro = assertThrows(OrdemServicoNaoEncontradaException.class,
                () -> ordemServicoConsultaService.buscarPorId(ordemId));

        assertEquals("Ordem de serviço não encontrada: " + ordemId, erro.getMessage());
        verify(ordemServicoMapper, never()).paraDetalhe(any());
    }
}
