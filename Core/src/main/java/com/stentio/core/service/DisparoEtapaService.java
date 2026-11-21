package com.stentio.core.service;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * US-013 CA.1 e US-014 CA.3: abre a etapa para os recursos elegíveis.
 *
 * <p>A elegibilidade não é regra nova: é a mesma consulta da US-012, que já cruza par de
 * idiomas, tipo de serviço, ativo e — desde a US-024 — certificação aprovada do par. A
 * US-014 reutiliza essa consulta como RN.4.
 */
@Service
public class DisparoEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final EtapaLiberacaoService liberacao;
    private final RecursoRepository recursos;
    private final ConviteEtapaEmailService convites;
    private final HistoricoEtapaRepository historico;
    private final Clock clock;

    public DisparoEtapaService(EtapaOrdemServicoRepository etapas,
                               EtapaLiberacaoService liberacao,
                               RecursoRepository recursos,
                               ConviteEtapaEmailService convites,
                               HistoricoEtapaRepository historico,
                               Clock clock) {
        this.etapas = etapas;
        this.liberacao = liberacao;
        this.recursos = recursos;
        this.convites = convites;
        this.historico = historico;
        this.clock = clock;
    }

    @Transactional
    public DisparoEtapaResponse disparar(UUID ordemServicoId, UUID etapaId, UsuarioAutenticado ator) {
        EtapaOrdemServico etapa = etapas.buscarParaAlocacao(ordemServicoId, etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        boolean estaLiberada = liberacao.estaLiberada(etapa);
        if (!estaLiberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + etapa.getNome() + " aguarda a entrega da etapa anterior");
        }

        List<Recurso> elegiveis = elegiveisPara(etapa);
        if (elegiveis.isEmpty()) {
            // Disparar para ninguém deixaria a etapa presa em DISPARADA esperando um
            // aceite que não pode vir. O gestor precisa saber agora, e alocar à mão ou
            // aprovar a certificação de alguém.
            throw new AlocacaoInvalidaException(
                    "Nenhum recurso com certificação aprovada atende o par de idiomas e o tipo de serviço desta etapa");
        }

        return efetivarDisparo(etapa, elegiveis, estaLiberada, ator.email());
    }

    @Transactional
    public Optional<DisparoEtapaResponse> dispararAutomaticamente(EtapaOrdemServico etapa) {
        boolean estaLiberada = liberacao.estaLiberada(etapa);
        if (!estaLiberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + etapa.getNome() + " aguarda a entrega da etapa anterior");
        }
        if (etapa.getStatus() != StatusEtapaOrdemServico.PENDENTE) {
            throw new TransicaoEtapaInvalidaException(
                    "O disparo automático exige uma etapa PENDENTE");
        }

        List<Recurso> elegiveis = elegiveisPara(etapa);
        if (elegiveis.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(efetivarDisparo(etapa, elegiveis, estaLiberada, "sistema"));
    }

    private DisparoEtapaResponse efetivarDisparo(EtapaOrdemServico etapa,
                                                 List<Recurso> elegiveis,
                                                 boolean estaLiberada,
                                                 String realizadoPor) {
        Instant agora = clock.instant();
        etapa.disparar(agora, estaLiberada);
        etapas.save(etapa);
        convites.convidar(etapa, elegiveis, agora);
        historico.save(new HistoricoEtapa(etapa, EventoEtapa.DISPARO, null, realizadoPor, agora));

        return new DisparoEtapaResponse(etapa.getId(), etapa.getStatus(), elegiveis.size());
    }

    /** RN.4: quem está apto a assumir esta etapa. */
    public List<Recurso> elegiveisPara(EtapaOrdemServico etapa) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        return recursos.buscarDisponiveis(
                solicitacao.getIdiomaOrigem().getId(),
                solicitacao.getIdiomaDestino().getId(),
                etapa.getTipoServico().getId());
    }
}
