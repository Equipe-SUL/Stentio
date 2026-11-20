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
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-013 CA.1: o gestor abre a etapa para quem pode executá-la.
 *
 * <p>A elegibilidade não é regra nova: é a mesma consulta da US-012, que já cruza par de
 * idiomas, tipo de serviço, ativo e verificado. A US-014 reutiliza essa consulta como RN.4.
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
            // verificar alguém.
            throw new AlocacaoInvalidaException(
                    "Nenhum recurso verificado atende o par de idiomas e o tipo de serviço desta etapa");
        }

        Instant agora = clock.instant();

        // A RN.6 mora na entidade: etapa já assumida ou entregue não volta para a fila.
        etapa.disparar(agora, estaLiberada);
        etapas.save(etapa);

        // CA.2: os convites entram na mesma transação do disparo. Em transações separadas,
        // daria para ter etapa disparada sem convite nenhum, ou convite de um disparo que
        // acabou falhando.
        convites.convidar(etapa, elegiveis, agora);

        // CA.8: sem recurso, porque o disparo não é dirigido a ninguém em particular.
        historico.save(new HistoricoEtapa(etapa, EventoEtapa.DISPARO, null, ator.email(), agora));

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
