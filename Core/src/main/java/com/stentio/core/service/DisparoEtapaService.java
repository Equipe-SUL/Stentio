package com.stentio.core.service;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.RecursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * US-013 CA.1: o gestor abre a etapa para quem pode executá-la.
 *
 * <p>A elegibilidade não é regra nova: é a mesma consulta da US-012, que já cruza par de
 * idiomas, tipo de serviço, ativo e verificado. A RN.1 e a RN.2 vieram de graça porque
 * aquela consulta já era a resposta para "quem pode pegar esta etapa".
 */
@Service
public class DisparoEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final RecursoRepository recursos;
    private final Clock clock;

    public DisparoEtapaService(EtapaOrdemServicoRepository etapas,
                               RecursoRepository recursos,
                               Clock clock) {
        this.etapas = etapas;
        this.recursos = recursos;
        this.clock = clock;
    }

    @Transactional
    public DisparoEtapaResponse disparar(UUID ordemServicoId, UUID etapaId) {
        EtapaOrdemServico etapa = etapas.buscarParaAlocacao(ordemServicoId, etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        List<Recurso> elegiveis = elegiveisPara(etapa);
        if (elegiveis.isEmpty()) {
            // Disparar para ninguém deixaria a etapa presa em DISPARADA esperando um
            // aceite que não pode vir. O gestor precisa saber agora, e alocar à mão ou
            // verificar alguém.
            throw new AlocacaoInvalidaException(
                    "Nenhum recurso verificado atende o par de idiomas e o tipo de serviço desta etapa");
        }

        // A RN.6 mora na entidade: etapa já assumida ou entregue não volta para a fila.
        etapa.disparar(clock.instant());
        etapas.save(etapa);

        return new DisparoEtapaResponse(etapa.getId(), etapa.getStatus(), elegiveis.size());
    }

    /** CA.1, RN.1 e RN.2: quem está apto a assumir esta etapa. */
    public List<Recurso> elegiveisPara(EtapaOrdemServico etapa) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        return recursos.buscarDisponiveis(
                solicitacao.getIdiomaOrigem().getId(),
                solicitacao.getIdiomaDestino().getId(),
                etapa.getTipoServico().getId());
    }
}
