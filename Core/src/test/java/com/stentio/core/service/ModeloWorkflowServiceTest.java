package com.stentio.core.service;

import com.stentio.core.dto.ModeloWorkflowRequest;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.ModeloWorkflowNaoEncontradoException;
import com.stentio.core.mapper.EtapaOrdemServicoMapper;
import com.stentio.core.model.ModeloWorkflow;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.ModeloWorkflowRepository;
import com.stentio.core.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// US-011: cadastro dos modelos de workflow. O card não descreve este fluxo; o contrato é proposta.
class ModeloWorkflowServiceTest {

    private final ModeloWorkflowRepository modelos = mock(ModeloWorkflowRepository.class);
    private final TipoServicoRepository tiposServico = mock(TipoServicoRepository.class);
    private final ModeloWorkflowService service =
            new ModeloWorkflowService(modelos, tiposServico, new EtapaOrdemServicoMapper());

    private TipoServico traducao;
    private TipoServico revisao;

    @BeforeEach
    void preparar() {
        traducao = tipo("Tradução");
        revisao = tipo("Revisão");
        when(tiposServico.findAllById(any())).thenReturn(List.of(traducao, revisao));
        when(modelos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static TipoServico tipo(String nome) {
        TipoServico t = new TipoServico();
        t.setId(UUID.randomUUID());
        t.setNome(nome);
        t.setAtivo(true);
        return t;
    }

    private ModeloWorkflowRequest pedido(String nome, ModeloWorkflowRequest.Etapa... etapas) {
        return new ModeloWorkflowRequest(nome, "Fluxo padrão", List.of(etapas));
    }

    private ModeloWorkflowRequest.Etapa etapa(int ordem, String nome, TipoServico tipo, int horas) {
        return new ModeloWorkflowRequest.Etapa(ordem, nome, tipo.getId(), horas);
    }

    @Test
    @DisplayName("Cadastra o modelo com as etapas ordenadas e o prazo de cada uma")
    void cadastra() {
        var resposta = service.criar(pedido("Juramentada padrão",
                etapa(2, "Revisão", revisao, 8),
                etapa(1, "Tradução", traducao, 20)));

        assertEquals("Juramentada padrão", resposta.nome());
        assertTrue(resposta.ativo());
        assertEquals(List.of(1, 2), resposta.etapas().stream().map(e -> e.ordem()).toList());
        assertEquals(20, resposta.etapas().getFirst().prazoEstimadoHoras());
        assertEquals("Tradução", resposta.etapas().getFirst().tipoServicoNome());
    }

    @Test
    @DisplayName("RN.1 também vale no modelo: lacuna é recusada")
    void recusaLacunaNoModelo() {
        assertThrows(EtapasInvalidasException.class, () -> service.criar(pedido("Com buraco",
                etapa(1, "Tradução", traducao, 20),
                etapa(3, "Revisão", revisao, 8))));

        verify(modelos, never()).save(any());
    }

    @Test
    @DisplayName("Nome repetido é recusado")
    void recusaNomeRepetido() {
        when(modelos.existsByNomeIgnoreCase(anyString())).thenReturn(true);

        assertThrows(EtapasInvalidasException.class,
                () -> service.criar(pedido("Juramentada padrão", etapa(1, "Tradução", traducao, 20))));
    }

    @Test
    @DisplayName("Desativar preserva o modelo, porque ele faz parte do histórico das OS")
    void desativa() {
        ModeloWorkflow modelo = new ModeloWorkflow("Antigo", null);
        UUID id = UUID.randomUUID();
        when(modelos.findWithEtapasById(id)).thenReturn(Optional.of(modelo));

        var resposta = service.alterarAtivacao(id, false);

        assertFalse(resposta.ativo());
        verify(modelos).save(modelo);
    }

    @Test
    @DisplayName("Modelo inexistente volta 404 pelo handler")
    void recusaInexistente() {
        UUID id = UUID.randomUUID();
        when(modelos.findWithEtapasById(id)).thenReturn(Optional.empty());

        assertThrows(ModeloWorkflowNaoEncontradoException.class, () -> service.buscarPorId(id));
    }
}
