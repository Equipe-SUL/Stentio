package com.stentio.core.service;

import com.stentio.core.dto.DashboardEtapaPendenciaProjection;
import com.stentio.core.dto.DashboardOrcamentoPendenciaProjection;
import com.stentio.core.dto.DashboardPendenciasResponse;
import com.stentio.core.dto.DashboardResumoResponse;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import com.stentio.core.support.CodigoVisual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<StatusOrcamento> STATUS_ORCAMENTO_PENDENTE = List.of(
            StatusOrcamento.EM_ELABORACAO,
            StatusOrcamento.AGUARDANDO_APROVACAO
    );
    private static final Comparator<DashboardPendenciasResponse.Pendencia> ORDENACAO_PENDENCIAS =
            Comparator.comparing(
                            DashboardPendenciasResponse.Pendencia::dataLimite,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(pendencia -> pendencia.tipoEntidade().name())
                    .thenComparing(pendencia -> pendencia.id().toString());

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

    public DashboardPendenciasResponse pendencias() {
        Instant agora = clock.instant();
        List<DashboardPendenciasResponse.Pendencia> pendencias = new ArrayList<>();

        for (DashboardOrcamentoPendenciaProjection orcamento
                : orcamentos.buscarPendenciasDashboard(STATUS_ORCAMENTO_PENDENTE)) {
            pendencias.add(new DashboardPendenciasResponse.Pendencia(
                    orcamento.id(),
                    DashboardPendenciasResponse.TipoEntidade.ORCAMENTO,
                    tituloOrcamento(orcamento),
                    orcamento.status().name(),
                    orcamento.dataExpiracao(),
                    "/orcamentos/" + orcamento.id()));
        }

        for (DashboardEtapaPendenciaProjection etapa : etapas.buscarPendenciasDashboard(agora)) {
            pendencias.add(new DashboardPendenciasResponse.Pendencia(
                    etapa.id(),
                    DashboardPendenciasResponse.TipoEntidade.ETAPA_OS,
                    tituloEtapa(etapa),
                    "ATRASADO",
                    etapa.dataLimiteEntrega(),
                    "/ordens/" + etapa.ordemServicoId()));
        }

        pendencias.sort(ORDENACAO_PENDENCIAS);
        return new DashboardPendenciasResponse(pendencias.size(), List.copyOf(pendencias));
    }

    private static String tituloOrcamento(DashboardOrcamentoPendenciaProjection orcamento) {
        String codigo = CodigoVisual.gerar("ORC", orcamento.criadoEm(), orcamento.id());
        return "Orçamento " + codigo + " - Cliente " + orcamento.nomeEmpresa();
    }

    private static String tituloEtapa(DashboardEtapaPendenciaProjection etapa) {
        String codigo = CodigoVisual.gerar("OS", etapa.ordemServicoCriadoEm(), etapa.ordemServicoId());
        return etapa.nome() + " - " + codigo;
    }

    private static double calcularTaxaConversao(long aprovados, long rejeitados) {
        long total = aprovados + rejeitados;
        if (total == 0L) {
            return 0.0;
        }
        return ((double) aprovados / total) * 100.0;
    }
}
