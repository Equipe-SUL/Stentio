package com.stentio.core.service;

import com.stentio.core.dto.ReprovacaoEntregaFinalResponse;
import com.stentio.core.event.EventosAuditoria;
import com.stentio.core.exception.ArquivoEntregueIndisponivelException;
import com.stentio.core.exception.TransicaoOrdemServicoInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-015 CA.10 a CA.12: o gestor reprova e a última etapa volta para quem a executou.
 *
 * <p>Nada vai para o cliente (RN.8). Quem precisa saber é o profissional, e ele é avisado
 * na mesma transação.
 */
@Service
public class ReprovacaoEntregaFinalService {

    private final EntregaFinalService revisao;
    private final EtapaOrdemServicoRepository etapas;
    private final EntregaFinalEmailOutboxRepository outbox;
    private final HistoricoEtapaRepository historico;
    private final ReaberturaEtapaEmailService avisos;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ReprovacaoEntregaFinalService(EntregaFinalService revisao,
                                         EtapaOrdemServicoRepository etapas,
                                         EntregaFinalEmailOutboxRepository outbox,
                                         HistoricoEtapaRepository historico,
                                         ReaberturaEtapaEmailService avisos,
                                         ApplicationEventPublisher eventos,
                                         Clock clock) {
        this.revisao = revisao;
        this.etapas = etapas;
        this.outbox = outbox;
        this.historico = historico;
        this.avisos = avisos;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public ReprovacaoEntregaFinalResponse reprovar(UUID ordemServicoId, String motivo,
                                                   UsuarioAutenticado ator) {
        OrdemServico ordem = revisao.buscarParaAtualizacao(ordemServicoId);

        // CA.12: a OS já encerrada cai aqui, porque sair de ENCERRADA não é transição válida.
        revisao.exigirAguardandoVerificacao(ordem);

        // CA.12: com envio aguardando confirmação, reprovar abriria a porta para o cliente
        // receber um pacote que a equipe acabou de recusar. É também o que garante que o
        // dispatcher nunca encontre a OS fora de AGUARDANDO_VERIFICACAO ao encerrar.
        if (outbox.existsByOrdemServicoIdAndEnviadoEmIsNull(ordemServicoId)
                || outbox.existsByOrdemServicoIdAndEnviadoEmIsNotNullAndEncerradoEmIsNull(ordemServicoId)) {
            throw new TransicaoOrdemServicoInvalidaException(
                    "Há um envio desta ordem de serviço aguardando confirmação; "
                            + "não é possível reprovar agora");
        }

        EtapaOrdemServico ultima = ultimaEntregue(ordemServicoId);
        Instant agora = clock.instant();

        String statusEtapaAnterior = EventosAuditoria.statusEtapa(ultima);
        StatusOrdemServico statusOrdemAnterior = ordem.getStatus();

        // CA.11: a etapa volta com recurso, alocação e prazo intactos (RN.9).
        ultima.reabrir();
        etapas.save(ultima);

        ordem.reprovarEntrega(motivo);

        // US-019: a OS e a etapa mudam juntas, e o motivo é o que explica as duas.
        eventos.publishEvent(EventosAuditoria.ordemServico(ordem, statusOrdemAnterior,
                "Entrega final reprovada: " + motivo, null));
        eventos.publishEvent(EventosAuditoria.etapa(ultima, statusEtapaAnterior,
                "Reaberta pela reprovação da entrega final: " + motivo, null));

        historico.save(new HistoricoEtapa(
                ultima, EventoEtapa.REABERTURA, ultima.getRecurso(), ator.email(), agora));

        avisos.avisarReabertura(ultima, motivo, agora);

        return new ReprovacaoEntregaFinalResponse(
                ultima.getId(), ultima.getStatus(), ordem.getStatus());
    }

    private EtapaOrdemServico ultimaEntregue(UUID ordemServicoId) {
        List<EtapaOrdemServico> entregues = revisao.entreguesEmOrdem(ordemServicoId);
        if (entregues.isEmpty()) {
            throw new ArquivoEntregueIndisponivelException(
                    "Nenhuma etapa desta ordem de serviço está entregue para ser reaberta");
        }
        return entregues.getLast();
    }
}
