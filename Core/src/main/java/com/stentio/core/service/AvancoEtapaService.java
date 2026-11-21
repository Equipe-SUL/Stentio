package com.stentio.core.service;

import com.stentio.core.event.EventosAuditoria;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.support.AtorAuditoria;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AvancoEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final ApplicationEventPublisher eventos;

    public AvancoEtapaService(EtapaOrdemServicoRepository etapas, ApplicationEventPublisher eventos) {
        this.etapas = etapas;
        this.eventos = eventos;
    }

    @Transactional
    public Resultado avancar(EtapaOrdemServico etapaEntregue) {
        OrdemServico ordemServico = etapaEntregue.getOrdemServico();
        StatusOrdemServico statusOsAnterior = ordemServico.getStatus();
        int proximaOrdem = SequenciaEtapas.proximaOrdem(etapaEntregue.getOrdem());
        Optional<EtapaOrdemServico> proxima = etapas.findByOrdemServicoAndOrdem(ordemServico, proximaOrdem);

        if (proxima.isEmpty()) {
            ordemServico.aguardarVerificacao();
            auditarOrdem(ordemServico, statusOsAnterior, "Última etapa entregue: OS aguardando verificação final.");
            return new Resultado(Optional.empty(), false);
        }

        if (etapaEntregue.getOrdem() == SequenciaEtapas.ORDEM_INICIAL) {
            ordemServico.iniciarExecucao();
            auditarOrdem(ordemServico, statusOsAnterior, "Primeira etapa entregue: OS em execução.");
        }

        EtapaOrdemServico etapaSeguinte = proxima.get();
        boolean liberadaNestaEntrega =
                etapaSeguinte.getStatus() == StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR;
        if (liberadaNestaEntrega) {
            String statusAnterior = EventosAuditoria.statusEtapa(etapaSeguinte);
            etapaSeguinte.liberar();
            etapas.save(etapaSeguinte);
            // US-019: o avanço é consequência da entrega, não ação de quem entregou.
            eventos.publishEvent(EventosAuditoria.etapa(etapaSeguinte, statusAnterior,
                    "Liberada pela entrega da etapa anterior.", AtorAuditoria.sistema()));
        }

        return new Resultado(Optional.of(etapaSeguinte), liberadaNestaEntrega);
    }

    /** O listener descarta o evento quando o status não mudou (ex.: OS já em execução). */
    private void auditarOrdem(OrdemServico ordem, StatusOrdemServico anterior, String observacao) {
        eventos.publishEvent(EventosAuditoria.ordemServico(ordem, anterior, observacao, AtorAuditoria.sistema()));
    }

    public record Resultado(Optional<EtapaOrdemServico> proximaEtapa, boolean liberadaNestaEntrega) {
    }
}
