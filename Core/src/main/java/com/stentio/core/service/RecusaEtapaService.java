package com.stentio.core.service;

import com.stentio.core.dto.EtapaStatusResponse;
import com.stentio.core.event.EventosAuditoria;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Recurso;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * US-013 CA.6 e RN.5: o profissional recusa a alocação direta.
 *
 * <p>Sem UPDATE condicional aqui: a etapa alocada tem um dono só, e não existe uma
 * segunda pessoa disputando a recusa. A guarda de estado mora na entidade.
 */
@Service
public class RecusaEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final EtapaLiberacaoService liberacao;
    private final RecursoAutenticadoService recursoAutenticado;
    private final RecusaEtapaEmailService avisos;
    private final HistoricoEtapaRepository historico;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RecusaEtapaService(EtapaOrdemServicoRepository etapas,
                              EtapaLiberacaoService liberacao,
                              RecursoAutenticadoService recursoAutenticado,
                              RecusaEtapaEmailService avisos,
                              HistoricoEtapaRepository historico,
                              ApplicationEventPublisher eventos,
                              Clock clock) {
        this.etapas = etapas;
        this.liberacao = liberacao;
        this.recursoAutenticado = recursoAutenticado;
        this.avisos = avisos;
        this.historico = historico;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public EtapaStatusResponse recusar(UUID etapaId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));
        Instant agora = clock.instant();

        if (!etapa.pertenceA(recurso)) {
            throw new AlocacaoInvalidaException("Esta etapa não está alocada a você");
        }

        boolean estaLiberada = liberacao.estaLiberada(etapa);

        // CA.8: montado antes de recusar(), que apaga o prazo que o histórico registra.
        HistoricoEtapa registro =
                new HistoricoEtapa(etapa, EventoEtapa.RECUSA, recurso, ator.email(), agora);

        String statusAnterior = EventosAuditoria.statusEtapa(etapa);

        // RN.1: recusada volta à fila apenas se a etapa já estiver liberada.
        etapa.recusar(estaLiberada);
        etapas.save(etapa);
        historico.save(registro);
        eventos.publishEvent(EventosAuditoria.etapa(etapa, statusAnterior,
                "Alocação recusada por " + recurso.getNome() + ".", null));

        // CA.6: na mesma transação. Equipe avisada só se a recusa valeu.
        avisos.avisarRecusa(etapa, recurso, agora);

        return new EtapaStatusResponse(etapa.getId(), etapa.getStatus());
    }
}
