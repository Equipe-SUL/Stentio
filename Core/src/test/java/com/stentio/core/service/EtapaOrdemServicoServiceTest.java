package com.stentio.core.service;

import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.exception.EtapasBloqueadasException;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.ModeloWorkflowNaoEncontradoException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.mapper.EtapaOrdemServicoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.ModeloWorkflow;
import com.stentio.core.model.ModeloWorkflowEtapa;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.ModeloWorkflowRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import com.stentio.core.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// US-011: orquestração dos dois caminhos de montagem e as regras RN.1 a RN.3.
class EtapaOrdemServicoServiceTest {

    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final ModeloWorkflowRepository modelos = mock(ModeloWorkflowRepository.class);
    private final TipoServicoRepository tiposServico = mock(TipoServicoRepository.class);

    private final EtapaOrdemServicoService service = new EtapaOrdemServicoService(
            ordens, etapas, modelos, tiposServico, new EtapaOrdemServicoMapper());

    private final UUID osId = UUID.randomUUID();
    private OrdemServico ordem;
    private TipoServico traducao;
    private TipoServico revisao;

    @BeforeEach
    void preparar() {
        ordem = mock(OrdemServico.class);
        when(ordem.getId()).thenReturn(osId);
        when(ordem.getStatus()).thenReturn(StatusOrdemServico.EM_PLANEJAMENTO);
        when(ordens.findById(osId)).thenReturn(Optional.of(ordem));

        traducao = tipo("Tradução");
        revisao = tipo("Revisão");
        when(tiposServico.findAllById(any())).thenReturn(List.of(traducao, revisao));
        when(etapas.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static TipoServico tipo(String nome) {
        TipoServico t = new TipoServico();
        t.setId(UUID.randomUUID());
        t.setNome(nome);
        t.setAtivo(true);
        return t;
    }

    private EtapasOrdemServicoRequest pedido(EtapasOrdemServicoRequest.Etapa... etapasPedidas) {
        return new EtapasOrdemServicoRequest(List.of(etapasPedidas));
    }

    private EtapasOrdemServicoRequest.Etapa etapa(int ordem, String nome, TipoServico tipo, int horas) {
        return new EtapasOrdemServicoRequest.Etapa(ordem, nome, tipo.getId(), horas);
    }

    @Test
    @DisplayName("Cenário 1: monta a sequência manualmente, coloca a OS em planejamento e aplica a CA.4")
    void montaSequenciaManual() {
        var resposta = service.definirManualmente(osId, pedido(
                etapa(1, "Tradução Principal", traducao, 24),
                etapa(2, "Revisão Final", revisao, 12)));

        verify(etapas).apagarPorOrdemServico(osId);
        verify(ordem).planejar();
        assertEquals(StatusOrdemServico.EM_PLANEJAMENTO, resposta.statusOs());
        assertEquals(2, resposta.etapas().size());

        var primeira = resposta.etapas().getFirst();
        assertEquals(1, primeira.ordem());
        assertEquals("Tradução Principal", primeira.nome());
        assertEquals(StatusEtapaOrdemServico.PENDENTE, primeira.status());
        assertEquals(24, primeira.prazoEstimadoHoras());
        assertEquals(traducao.getId(), primeira.tipoServicoId());
        assertEquals("Tradução", primeira.tipoServicoNome());
        assertNull(primeira.recursoId());

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, resposta.etapas().get(1).status());
    }

    @Test
    @DisplayName("A ordem de chegada não importa: a sequência é gravada ordenada")
    void ordenaAntesDeGravar() {
        var resposta = service.definirManualmente(osId, pedido(
                etapa(2, "Revisão Final", revisao, 12),
                etapa(1, "Tradução Principal", traducao, 24)));

        assertEquals(List.of(1, 2), resposta.etapas().stream().map(e -> e.ordem()).toList());
        assertEquals(StatusEtapaOrdemServico.PENDENTE, resposta.etapas().getFirst().status());
    }

    @Test
    @DisplayName("RN.1: lacuna na numeração não chega a tocar o banco")
    void recusaLacunaSemGravar() {
        assertThrows(EtapasInvalidasException.class, () -> service.definirManualmente(osId, pedido(
                etapa(1, "Tradução", traducao, 24),
                etapa(3, "Revisão", revisao, 12))));

        verify(etapas, never()).apagarPorOrdemServico(any());
        verify(etapas, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("RN.3: sequência com etapa alocada não pode ser remontada")
    void recusaRemontagemAposAlocacao() {
        when(etapas.existsByOrdemServicoIdAndRecursoIsNotNull(osId)).thenReturn(true);

        var erro = assertThrows(EtapasBloqueadasException.class,
                () -> service.definirManualmente(osId, pedido(etapa(1, "Tradução", traducao, 24))));

        assertTrue(erro.getMessage().contains("alocada"), erro.getMessage());
        verify(etapas, never()).apagarPorOrdemServico(any());
    }

    @Test
    @DisplayName("Tipo de serviço inexistente vira erro de entrada, não erro de banco")
    void recusaTipoServicoInexistente() {
        when(tiposServico.findAllById(any())).thenReturn(List.of());

        assertThrows(EtapasInvalidasException.class,
                () -> service.definirManualmente(osId, pedido(etapa(1, "Tradução", traducao, 24))));
    }

    @Test
    @DisplayName("Tipo de serviço desativado não pode ser usado em etapa nova")
    void recusaTipoServicoInativo() {
        traducao.setAtivo(false);
        when(tiposServico.findAllById(any())).thenReturn(List.of(traducao));

        var erro = assertThrows(EtapasInvalidasException.class,
                () -> service.definirManualmente(osId, pedido(etapa(1, "Tradução", traducao, 24))));

        assertTrue(erro.getMessage().contains("inativo"), erro.getMessage());
    }

    @Test
    @DisplayName("CA.2: aplicar um modelo clona nome, tipo, ordem e prazo de cada etapa")
    void aplicaModelo() {
        ModeloWorkflow modelo = new ModeloWorkflow("Juramentada padrão", "Fluxo completo");
        modelo.definirEtapas(List.of(
                new ModeloWorkflowEtapa(modelo, 1, "Tradução", traducao, 20),
                new ModeloWorkflowEtapa(modelo, 2, "Revisão", revisao, 8)));
        UUID modeloId = UUID.randomUUID();
        when(modelos.findWithEtapasById(modeloId)).thenReturn(Optional.of(modelo));

        var resposta = service.aplicarModelo(osId, modeloId);

        verify(etapas).apagarPorOrdemServico(osId);
        verify(ordem).planejar();
        assertEquals(2, resposta.etapas().size());
        assertEquals("Tradução", resposta.etapas().getFirst().nome());
        assertEquals(20, resposta.etapas().getFirst().prazoEstimadoHoras());
        assertEquals(StatusEtapaOrdemServico.PENDENTE, resposta.etapas().getFirst().status());
        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, resposta.etapas().get(1).status());
        assertEquals(8, resposta.etapas().get(1).prazoEstimadoHoras());
    }

    @Test
    @DisplayName("Modelo inativo não é aplicável")
    void recusaModeloInativo() {
        ModeloWorkflow modelo = new ModeloWorkflow("Antigo", null);
        modelo.definirEtapas(List.of(new ModeloWorkflowEtapa(modelo, 1, "Tradução", traducao, 20)));
        modelo.ativar(false);
        UUID modeloId = UUID.randomUUID();
        when(modelos.findWithEtapasById(modeloId)).thenReturn(Optional.of(modelo));

        assertThrows(ModeloWorkflowNaoEncontradoException.class, () -> service.aplicarModelo(osId, modeloId));
        verify(etapas, never()).apagarPorOrdemServico(any());
    }

    @Test
    @DisplayName("Modelo inexistente devolve 404 pelo handler")
    void recusaModeloInexistente() {
        UUID modeloId = UUID.randomUUID();
        when(modelos.findWithEtapasById(modeloId)).thenReturn(Optional.empty());

        assertThrows(ModeloWorkflowNaoEncontradoException.class, () -> service.aplicarModelo(osId, modeloId));
    }

    @Test
    @DisplayName("Ordem de serviço inexistente é 404, mesmo antes de olhar as etapas")
    void recusaOrdemInexistente() {
        UUID desconhecida = UUID.randomUUID();
        when(ordens.findById(desconhecida)).thenReturn(Optional.empty());

        assertThrows(OrdemServicoNaoEncontradaException.class,
                () -> service.definirManualmente(desconhecida, pedido(etapa(1, "Tradução", traducao, 24))));
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> service.listar(desconhecida));
    }

    @Test
    @DisplayName("A listagem devolve as etapas na ordem de execução")
    void lista() {
        when(etapas.listarPorOrdemServico(osId)).thenReturn(List.of(
                new EtapaOrdemServico(ordem, 1, "Tradução", traducao, 24),
                new EtapaOrdemServico(ordem, 2, "Revisão", revisao, 12)));

        var resposta = service.listar(osId);

        assertEquals(List.of(1, 2), resposta.etapas().stream().map(e -> e.ordem()).toList());
        assertEquals(osId, resposta.ordemServicoId());
    }
}
