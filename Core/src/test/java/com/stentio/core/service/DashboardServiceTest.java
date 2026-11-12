package com.stentio.core.service;

import com.stentio.core.dto.DashboardResumoResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(orcamentos.countByStatus(StatusOrcamento.EM_ELABORACAO)).thenReturn(5L);
        when(orcamentos.countByStatus(StatusOrcamento.AGUARDANDO_APROVACAO)).thenReturn(3L);
        when(orcamentos.countByStatus(StatusOrcamento.APROVADO)).thenReturn(68L);
        when(orcamentos.countByStatus(StatusOrcamento.REJEITADO)).thenReturn(32L);
        when(ordensServico.countByStatus(StatusOrdemServico.EM_PLANEJAMENTO)).thenReturn(12L);
        when(etapas.countByDataLimiteEntregaBeforeAndRecursoIsNotNull(AGORA)).thenReturn(2L);
    }

    @Test
    void deveCalcularResumoDoDashboard() {
        DashboardResumoResponse resumo = dashboardService.resumo();

        assertEquals(5, resumo.totalOrcamentosEmElaboracao());
        assertEquals(3, resumo.totalOrcamentosAguardandoAprovacao());
        assertEquals(12, resumo.totalOsEmAndamento());
        assertEquals(2, resumo.totalEtapasAtrasadas());
        assertEquals(68.0, resumo.taxaConversaoOrcamentos());
        verify(etapas).countByDataLimiteEntregaBeforeAndRecursoIsNotNull(AGORA);
    }
}
