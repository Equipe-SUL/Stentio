package com.stentio.core.service;

import org.springframework.context.ApplicationEventPublisher;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import com.stentio.core.event.StatusAlteradoEvento;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.support.AtorAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvancoEtapaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-27T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final AvancoEtapaService service = new AvancoEtapaService(etapas, eventos);

    private OrdemServico ordemServico;
    private TipoServico tipoServico;
    private EtapaOrdemServico entregue;

    @BeforeEach
    void preparar() {
        Orcamento orcamento = mock(Orcamento.class);
        when(orcamento.getSolicitacao()).thenReturn(mock(Solicitacao.class));
        ordemServico = new OrdemServico(orcamento);
        // US-019: os eventos de auditoria identificam a OS pelo id, que normalmente vem do banco.
        ReflectionTestUtils.setField(ordemServico, "id", UUID.randomUUID());
        ordemServico.planejar();

        tipoServico = new TipoServico();
        tipoServico.setId(UUID.randomUUID());
        tipoServico.setNome("Tradução");
        entregue = etapaEntregue(1);
    }

    @Test
    void liberaProximaEtapaEIniciaExecucaoDaOrdem() {
        EtapaOrdemServico proxima = novaEtapa(2);
        when(etapas.findByOrdemServicoAndOrdem(ordemServico, 2)).thenReturn(Optional.of(proxima));

        AvancoEtapaService.Resultado resultado = service.avancar(entregue);

        assertSame(proxima, resultado.proximaEtapa().orElseThrow());
        assertTrue(resultado.liberadaNestaEntrega());
        assertEquals(StatusEtapaOrdemServico.PENDENTE, proxima.getStatus());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordemServico.getStatus());
        verify(etapas).save(proxima);
    }

    @Test
    @DisplayName("US-019: o avanço audita a OS em execução e a etapa liberada, como Sistema")
    void auditaOAvancoComoSistema() {
        EtapaOrdemServico proxima = novaEtapa(2);
        when(etapas.findByOrdemServicoAndOrdem(ordemServico, 2)).thenReturn(Optional.of(proxima));

        service.avancar(entregue);

        ArgumentCaptor<StatusAlteradoEvento> captor = ArgumentCaptor.forClass(StatusAlteradoEvento.class);
        verify(eventos, times(2)).publishEvent(captor.capture());
        StatusAlteradoEvento daOrdem = captor.getAllValues().get(0);
        StatusAlteradoEvento daEtapa = captor.getAllValues().get(1);

        assertEquals(EntidadeAuditada.ORDEM_SERVICO, daOrdem.entidade());
        assertEquals("EM_PLANEJAMENTO", daOrdem.statusAnterior());
        assertEquals("EM_EXECUCAO", daOrdem.statusNovo());
        assertEquals(EntidadeAuditada.ORDEM_SERVICO_ETAPA, daEtapa.entidade());
        assertEquals("AGUARDANDO_ETAPA_ANTERIOR", daEtapa.statusAnterior());
        assertEquals("PENDENTE", daEtapa.statusNovo());
        assertEquals(AtorAuditoria.PERFIL_SISTEMA, daOrdem.ator().perfil());
        assertEquals(AtorAuditoria.PERFIL_SISTEMA, daEtapa.ator().perfil());
    }

    @Test
    void preservaConviteNominalDaProximaEtapa() {
        EtapaOrdemServico proxima = novaEtapa(2);
        proxima.alocar(recursoElegivel(), PRAZO, AGORA, true);
        when(etapas.findByOrdemServicoAndOrdem(ordemServico, 2)).thenReturn(Optional.of(proxima));

        service.avancar(entregue);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, proxima.getStatus());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordemServico.getStatus());
    }

    @Test
    void preservaAceiteNominalAntecipadoDaProximaEtapa() {
        EtapaOrdemServico proxima = novaEtapa(2);
        Recurso recurso = recursoElegivel();
        proxima.alocar(recurso, PRAZO, AGORA, true);
        proxima.aceitarPor(recurso, AGORA, false);
        when(etapas.findByOrdemServicoAndOrdem(ordemServico, 2)).thenReturn(Optional.of(proxima));

        service.avancar(entregue);

        assertEquals(StatusEtapaOrdemServico.ACEITA, proxima.getStatus());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordemServico.getStatus());
    }

    @Test
    void ultimaEtapaDeOrdemDeUmaEtapaMoveOrdemParaAguardandoVerificacao() {
        when(etapas.findByOrdemServicoAndOrdem(ordemServico, 2)).thenReturn(Optional.empty());

        AvancoEtapaService.Resultado resultado = service.avancar(entregue);

        assertTrue(resultado.proximaEtapa().isEmpty());
        assertFalse(resultado.liberadaNestaEntrega());
        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, ordemServico.getStatus());
    }

    private EtapaOrdemServico etapaEntregue(int ordem) {
        EtapaOrdemServico etapa = novaEtapa(ordem);
        Recurso recurso = recursoElegivel();
        etapa.alocar(recurso, PRAZO, AGORA, true);
        etapa.aceitarPor(recurso, AGORA, true);
        etapa.entregar("entrega.docx", "2026/11/entrega.docx", AGORA, true);
        return etapa;
    }

    private EtapaOrdemServico novaEtapa(int ordem) {
        EtapaOrdemServico etapa = new EtapaOrdemServico(ordemServico, ordem, "Etapa " + ordem, tipoServico, 24);
        ReflectionTestUtils.setField(etapa, "id", UUID.randomUUID());
        return etapa;
    }

    private static Recurso recursoElegivel() {
        Recurso recurso = mock(Recurso.class);
        when(recurso.isAtivo()).thenReturn(true);
        return recurso;
    }
}
