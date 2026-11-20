package com.stentio.core.service;

import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AvancoEtapaService {

    private final EtapaOrdemServicoRepository etapas;

    public AvancoEtapaService(EtapaOrdemServicoRepository etapas) {
        this.etapas = etapas;
    }

    @Transactional
    public Optional<EtapaOrdemServico> avancar(EtapaOrdemServico etapaEntregue) {
        OrdemServico ordemServico = etapaEntregue.getOrdemServico();
        int proximaOrdem = SequenciaEtapas.proximaOrdem(etapaEntregue.getOrdem());
        Optional<EtapaOrdemServico> proxima = etapas.findByOrdemServicoAndOrdem(ordemServico, proximaOrdem);

        if (proxima.isEmpty()) {
            ordemServico.aguardarVerificacao();
            return Optional.empty();
        }

        if (etapaEntregue.getOrdem() == SequenciaEtapas.ORDEM_INICIAL) {
            ordemServico.iniciarExecucao();
        }

        EtapaOrdemServico etapaSeguinte = proxima.get();
        if (etapaSeguinte.getStatus() == StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR) {
            etapaSeguinte.liberar();
            etapas.save(etapaSeguinte);
        }

        return Optional.of(etapaSeguinte);
    }
}
