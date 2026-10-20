package com.stentio.core.service;

import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {
    @Mock OrcamentoRepository orcamentos;
    @Mock OrdemServicoRepository ordensServico;
    @InjectMocks OrdemServicoService service;
    private Orcamento orcamento;

    @BeforeEach
    void preparar() {
        orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        orcamento.setStatus(StatusOrcamento.APROVADO);
        orcamento.setSolicitacao(mock(Solicitacao.class));
    }

    @Test
    void criaOrdemComVinculosDoBancoEStatusPendenteAlocacao() {
        when(orcamentos.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordensServico.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico ordem = service.criarParaOrcamentoAprovado(orcamento.getId());

        assertSame(orcamento, ordem.getOrcamento());
        assertSame(orcamento.getSolicitacao(), ordem.getSolicitacao());
        assertEquals(StatusOrdemServico.PENDENTE_ALOCACAO, ordem.getStatus());
        var ordemDasConsultas = inOrder(orcamentos, ordensServico);
        ordemDasConsultas.verify(orcamentos).buscarPorIdComLock(orcamento.getId());
        ordemDasConsultas.verify(ordensServico).findByOrcamentoId(orcamento.getId());
        ordemDasConsultas.verify(ordensServico).save(ordem);
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrcamento.class, names = "APROVADO", mode = EnumSource.Mode.EXCLUDE)
    void recusaTodosOsStatusNaoAprovados(StatusOrcamento status) {
        orcamento.setStatus(status);
        when(orcamentos.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));

        assertThrows(OrcamentoInvalidoException.class,
                () -> service.criarParaOrcamentoAprovado(orcamento.getId()));
        verifyNoInteractions(ordensServico);
    }

    @Test
    void devolveOrdemExistenteSemPersistirDuplicata() {
        OrdemServico existente = new OrdemServico(orcamento);
        when(orcamentos.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordensServico.findByOrcamentoId(orcamento.getId())).thenReturn(Optional.of(existente));

        assertSame(existente, service.criarParaOrcamentoAprovado(orcamento.getId()));
        verify(ordensServico, never()).save(any());
    }

    @Test
    void recusaOrcamentoLegadoSemSolicitacao() {
        orcamento.setSolicitacao(null);
        when(orcamentos.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));

        assertThrows(OrcamentoInvalidoException.class,
                () -> service.criarParaOrcamentoAprovado(orcamento.getId()));
        verify(ordensServico, never()).save(any());
    }

    @Test
    void recusaOrcamentoInexistente() {
        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> service.criarParaOrcamentoAprovado(orcamento.getId()));
        verifyNoInteractions(ordensServico);
    }

    @Test
    void recusaIdentificadorAusente() {
        assertThrows(OrcamentoInvalidoException.class, () -> service.criarParaOrcamentoAprovado(null));
        verifyNoInteractions(orcamentos, ordensServico);
    }
}
