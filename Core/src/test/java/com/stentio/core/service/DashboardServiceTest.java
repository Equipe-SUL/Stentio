package com.stentio.core.service;

import com.stentio.core.dto.DashboardResumoResponse;
import com.stentio.core.dto.DashboardEtapaPendenciaProjection;
import com.stentio.core.dto.DashboardOrcamentoPendenciaProjection;
import com.stentio.core.dto.DashboardPendenciasResponse;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T18:00:00Z");

    @Mock
    private OrcamentoRepository orcamentos;

    @Mock
    private OrdemServicoRepository ordensServico;

    @Mock
    private EtapaOrdemServicoRepository etapas;

    @Mock
    private Clock clock;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(orcamentos, ordensServico, etapas, clock);
        when(clock.instant()).thenReturn(AGORA);
    }

    @Test
    void deveCalcularResumoDoDashboard() {
        when(orcamentos.countByStatus(StatusOrcamento.EM_ELABORACAO)).thenReturn(5L);
        when(orcamentos.countByStatus(StatusOrcamento.AGUARDANDO_APROVACAO)).thenReturn(3L);
        when(orcamentos.countByStatus(StatusOrcamento.APROVADO)).thenReturn(68L);
        when(orcamentos.countByStatus(StatusOrcamento.REJEITADO)).thenReturn(32L);
        when(ordensServico.countByStatus(StatusOrdemServico.EM_PLANEJAMENTO)).thenReturn(12L);
        when(etapas.countByDataLimiteEntregaBeforeAndRecursoIsNotNull(AGORA)).thenReturn(2L);

        DashboardResumoResponse resumo = dashboardService.resumo();

        assertEquals(5, resumo.totalOrcamentosEmElaboracao());
        assertEquals(3, resumo.totalOrcamentosAguardandoAprovacao());
        assertEquals(12, resumo.totalOsEmAndamento());
        assertEquals(2, resumo.totalEtapasAtrasadas());
        assertEquals(68.0, resumo.taxaConversaoOrcamentos());
        verify(etapas).countByDataLimiteEntregaBeforeAndRecursoIsNotNull(AGORA);
    }

    @Test
    void deveConsolidarPendenciasEOrdenarPorDataLimite() {
        Instant vencimentoEtapa = AGORA.minusSeconds(3600);
        Instant vencimentoOrcamento = AGORA.plusSeconds(3600);
        var ordemId = java.util.UUID.randomUUID();
        var orcamentoId = java.util.UUID.randomUUID();
        var etapaId = java.util.UUID.randomUUID();
        var semVencimentoId = java.util.UUID.randomUUID();

        when(orcamentos.buscarPendenciasDashboard(List.of(
                StatusOrcamento.EM_ELABORACAO, StatusOrcamento.AGUARDANDO_APROVACAO)))
                .thenReturn(List.of(
                        new DashboardOrcamentoPendenciaProjection(
                                semVencimentoId,
                                StatusOrcamento.AGUARDANDO_APROVACAO,
                                null,
                                AGORA,
                                "Cliente sem prazo"),
                        new DashboardOrcamentoPendenciaProjection(
                                orcamentoId,
                                StatusOrcamento.EM_ELABORACAO,
                                vencimentoOrcamento,
                                AGORA,
                                "Cliente ACME")));
        when(etapas.buscarPendenciasDashboard(AGORA)).thenReturn(List.of(
                new DashboardEtapaPendenciaProjection(
                        etapaId, "Tradução Técnica", vencimentoEtapa, ordemId, AGORA)));

        DashboardPendenciasResponse resposta = dashboardService.pendencias();

        assertEquals(3, resposta.totalItens());
        assertEquals(List.of(etapaId, orcamentoId, semVencimentoId),
                resposta.pendencias().stream().map(DashboardPendenciasResponse.Pendencia::id).toList());
        assertEquals(DashboardPendenciasResponse.TipoEntidade.ETAPA_OS,
                resposta.pendencias().getFirst().tipoEntidade());
        assertEquals("ATRASADO", resposta.pendencias().getFirst().status());
        assertEquals("/ordens/" + ordemId, resposta.pendencias().getFirst().linkAcao());
        assertEquals(DashboardPendenciasResponse.TipoEntidade.ORCAMENTO,
                resposta.pendencias().get(1).tipoEntidade());
        assertEquals("EM_ELABORACAO", resposta.pendencias().get(1).status());
        assertEquals("/orcamentos/" + orcamentoId, resposta.pendencias().get(1).linkAcao());
        assertNull(resposta.pendencias().getLast().dataLimite());
    }

    @Test
    void taxaConversaoSemDecisoesDeveSerZero() {
        when(orcamentos.countByStatus(StatusOrcamento.EM_ELABORACAO)).thenReturn(0L);
        when(orcamentos.countByStatus(StatusOrcamento.AGUARDANDO_APROVACAO)).thenReturn(0L);
        when(orcamentos.countByStatus(StatusOrcamento.APROVADO)).thenReturn(0L);
        when(orcamentos.countByStatus(StatusOrcamento.REJEITADO)).thenReturn(0L);

        DashboardResumoResponse resposta = dashboardService.resumo();

        assertEquals(0.0, resposta.taxaConversaoOrcamentos());
    }
}
