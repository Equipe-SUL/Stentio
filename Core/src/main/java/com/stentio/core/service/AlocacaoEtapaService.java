package com.stentio.core.service;

import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.AlocacaoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.model.EtapaOrdemServico;
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
import java.util.UUID;

/**
 * US-012: vincula um profissional a uma etapa da ordem de serviço.
 *
 * <p>A compatibilidade é conferida de novo aqui, mesmo que a tela só liste quem o endpoint
 * de disponíveis devolveu: entre listar e confirmar, alguém pode desativar o recurso ou
 * tirar uma competência dele. Quem decide é o servidor, não a lista que o front guardou.
 */
@Service
public class AlocacaoEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final RecursoRepository recursos;
    private final HistoricoEtapaRepository historico;
    private final Clock clock;

    public AlocacaoEtapaService(EtapaOrdemServicoRepository etapas,
                                RecursoRepository recursos,
                                HistoricoEtapaRepository historico,
                                Clock clock) {
        this.etapas = etapas;
        this.recursos = recursos;
        this.historico = historico;
        this.clock = clock;
    }

    // CA.3 e CA.4, Cenário 1
    @Transactional
    public AlocacaoEtapaResponse alocar(UUID ordemServicoId, UUID etapaId,
                                        AlocacaoEtapaRequest requisicao, UsuarioAutenticado ator) {
        EtapaOrdemServico etapa = etapas.buscarParaAlocacao(ordemServicoId, etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        // RN.3 antes de qualquer validação de compatibilidade: se nem pode trocar, de nada
        // adianta saber se o recurso serviria.
        etapa.garantirQuePodeTrocarRecurso(ator.administrador());

        Recurso recurso = recursos.buscarComCompetencias(requisicao.recursoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(requisicao.recursoId()));

        garantirCompetencia(etapa, recurso);

        // Lido antes de alocar: depois da troca, quem estava no lugar já se perdeu.
        Recurso anterior = etapa.getRecurso();

        Instant agora = clock.instant();
        etapa.alocar(recurso, requisicao.dataLimiteEntrega(), agora);
        etapas.save(etapa);

        // CA.4: o histórico entra na mesma transação da alocação. Em transações separadas,
        // uma falha aqui deixaria a etapa alocada sem rastro de quem a alocou.
        historico.save(new HistoricoEtapa(
                etapa, anterior, recurso, etapa.getDataLimiteEntrega(), ator.email(), agora));

        return AlocacaoEtapaResponse.de(etapa);
    }

    /** RN.1 e RN.2, com a mensagem dizendo qual das duas falhou — é o que o Cenário 2 pede. */
    private static void garantirCompetencia(EtapaOrdemServico etapa, Recurso recurso) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        UUID origemId = solicitacao.getIdiomaOrigem().getId();
        UUID destinoId = solicitacao.getIdiomaDestino().getId();

        // RN.1
        if (!recurso.atendePar(origemId, destinoId)) {
            throw new AlocacaoInvalidaException("%s não tem o par %s → %s nas competências"
                    .formatted(recurso.getNome(),
                            solicitacao.getIdiomaOrigem().getNome(),
                            solicitacao.getIdiomaDestino().getNome()));
        }

        // RN.2
        if (!recurso.executa(etapa.getTipoServico().getId())) {
            throw new AlocacaoInvalidaException("%s não atende o tipo de serviço %s"
                    .formatted(recurso.getNome(), etapa.getTipoServico().getNome()));
        }
    }
}
