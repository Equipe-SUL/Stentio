package com.stentio.core.service;

import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import org.springframework.stereotype.Service;

@Service
public class EtapaLiberacaoService {

    private final EtapaOrdemServicoRepository etapas;

    public EtapaLiberacaoService(EtapaOrdemServicoRepository etapas) {
        this.etapas = etapas;
    }

    public boolean estaLiberada(EtapaOrdemServico etapa) {
        if (etapa.getOrdem() == SequenciaEtapas.ORDEM_INICIAL) {
            return etapa.liberada(null);
        }
        if (etapa.getOrdem() < SequenciaEtapas.ORDEM_INICIAL) {
            return false;
        }

        StatusEtapaOrdemServico anterior = etapas.buscarStatusPorOrdem(
                etapa.getOrdemServico().getId(), etapa.getOrdem() - 1).orElse(null);
        return etapa.liberada(anterior);
    }
}
