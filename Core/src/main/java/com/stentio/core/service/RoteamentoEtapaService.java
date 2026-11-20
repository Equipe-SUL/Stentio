package com.stentio.core.service;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.StatusEtapaOrdemServico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RoteamentoEtapaService {

    private final AvancoEtapaService avanco;
    private final DisparoEtapaService disparo;

    public RoteamentoEtapaService(AvancoEtapaService avanco, DisparoEtapaService disparo) {
        this.avanco = avanco;
        this.disparo = disparo;
    }

    @Transactional
    public Resultado rotear(EtapaOrdemServico etapaEntregue) {
        AvancoEtapaService.Resultado resultadoAvanco = avanco.avancar(etapaEntregue);
        Optional<EtapaOrdemServico> proximaEtapa = resultadoAvanco.proximaEtapa();

        if (proximaEtapa.isEmpty()) {
            return new Resultado(Optional.empty(), false, 0);
        }

        EtapaOrdemServico proxima = proximaEtapa.orElseThrow();
        if (resultadoAvanco.liberadaNestaEntrega()) {
            Optional<DisparoEtapaResponse> resultadoDisparo = disparo.dispararAutomaticamente(proxima);
            return resultadoDisparo
                    .map(convite -> new Resultado(
                            proximaEtapa, true, convite.recursosNotificados()))
                    .orElseGet(() -> new Resultado(proximaEtapa, false, 0));
        }

        int recursosNotificados = proxima.getStatus() == StatusEtapaOrdemServico.ALOCADO
                || proxima.getStatus() == StatusEtapaOrdemServico.ACEITA
                ? 1
                : 0;
        return new Resultado(proximaEtapa, false, recursosNotificados);
    }

    public record Resultado(
            Optional<EtapaOrdemServico> proximaEtapa,
            boolean conviteAutomatico,
            int recursosNotificados
    ) {
    }
}
