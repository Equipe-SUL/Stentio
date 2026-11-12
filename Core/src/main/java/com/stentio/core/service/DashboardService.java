package com.stentio.core.service;

import com.stentio.core.dto.DashboardResumoResponse;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrcamentoRepository orcamentos;
    private final OrdemServicoRepository ordensServico;
    private final EtapaOrdemServicoRepository etapas;
    private final Clock clock;

    public DashboardService(OrcamentoRepository orcamentos,
                            OrdemServicoRepository ordensServico,
                            EtapaOrdemServicoRepository etapas,
                            Clock clock) {
        this.orcamentos = orcamentos;
        this.ordensServico = ordensServico;
        this.etapas = etapas;
        this.clock = clock;
    }

    public DashboardResumoResponse resumo() {
        long emElaboracao = orcamentos.countByStatus(StatusOrcamento.EM_ELABORACAO);
        long aguardandoAprovacao = orcamentos.countByStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
        long osEmAndamento = ordensServico.countByStatus(StatusOrdemServico.EM_PLANEJAMENTO);
        long etapasAtrasadas = etapas.countByDataLimiteEntregaBeforeAndRecursoIsNotNull(clock.instant());

        long aprovados = orcamentos.countByStatus(StatusOrcamento.APROVADO);
        long rejeitados = orcamentos.countByStatus(StatusOrcamento.REJEITADO);
        double taxaConversao = calcularTaxaConversao(aprovados, rejeitados);

        return new DashboardResumoResponse(
                emElaboracao,
                aguardandoAprovacao,
                osEmAndamento,
                etapasAtrasadas,
                taxaConversao
        );
    }

    private static double calcularTaxaConversao(long aprovados, long rejeitados) {
        long total = aprovados + rejeitados;
        if (total == 0L) {
            return 0.0;
        }
        return ((double) aprovados / total) * 100.0;
    }
}
