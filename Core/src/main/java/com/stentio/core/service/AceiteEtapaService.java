package com.stentio.core.service;

import com.stentio.core.dto.AceiteEtapaResponse;
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
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * US-013 CA.3 e RN.3: quem aceita primeiro fica com a etapa.
 *
 * <p>A conferência de elegibilidade e o aceite em si são coisas diferentes. A primeira
 * responde "esta etapa é para você?" e pode ser feita lendo. A segunda é uma disputa entre
 * dois candidatos simultâneos e não pode ser resolvida lendo — por isso vai inteira para o
 * UPDATE condicional do repositório.
 */
@Service
public class AceiteEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final RecursoAutenticadoService recursoAutenticado;
    private final HistoricoEtapaRepository historico;
    private final Clock clock;

    public AceiteEtapaService(EtapaOrdemServicoRepository etapas,
                              RecursoAutenticadoService recursoAutenticado,
                              HistoricoEtapaRepository historico,
                              Clock clock) {
        this.etapas = etapas;
        this.recursoAutenticado = recursoAutenticado;
        this.historico = historico;
        this.clock = clock;
    }

    @Transactional
    public AceiteEtapaResponse aceitar(UUID etapaId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        StatusEtapaOrdemServico esperado = estadoDeOndeSaiOAceite(etapa, recurso);

        // CA.3: o status esperado entra na condição do UPDATE. Quem perder a corrida
        // atualiza zero linhas, e zero linhas é a resposta 409 — não um aceite silencioso
        // por cima do colega.
        int atualizadas = etapas.aceitarSeDisponivel(
                etapaId,
                recurso,
                clock.instant(),
                StatusEtapaOrdemServico.ACEITA,
                esperado);

        if (atualizadas == 0) {
            throw new TransicaoEtapaInvalidaException(
                    "Esta etapa mudou de estado antes do seu aceite. Recarregue a lista");
        }

        // O UPDATE em massa limpa o contexto de persistência, então a etapa em memória
        // está velha: a resposta vem de uma leitura nova.
        EtapaOrdemServico aceita = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        // CA.8: o rastro sai depois do UPDATE vencedor, nunca da tentativa.
        historico.save(new HistoricoEtapa(
                aceita, EventoEtapa.ACEITE, recurso, ator.email(), aceita.getAceitoEm()));

        return new AceiteEtapaResponse(
                aceita.getId(),
                aceita.getStatus(),
                aceita.getAceitoEm(),
                aceita.getDataLimiteEntrega());
    }

    /**
     * De que estado este aceite pode sair — e, portanto, qual condição vai no UPDATE.
     *
     * <p>São dois caminhos até a mesma etapa. No convite aberto (DISPARADA) qualquer
     * elegível pode aceitar, e a conferência é de perfil. Na alocação direta (ALOCADO) o
     * gestor já escolheu a pessoa, e a conferência é de dono: aceitar o convite de outro
     * não é questão de competência.
     */
    private StatusEtapaOrdemServico estadoDeOndeSaiOAceite(EtapaOrdemServico etapa, Recurso recurso) {
        if (!recurso.elegivelParaAlocacao()) {
            throw new AlocacaoInvalidaException(
                    "Seu cadastro ainda não foi verificado pela equipe da Aliança");
        }

        return switch (etapa.getStatus()) {
            case DISPARADA -> {
                // US-014 CA.1: convite aberto só se aceita em etapa liberada. A anterior
                // entregue não volta atrás, então conferir aqui, antes do UPDATE, basta.
                etapa.garantirLiberada(etapas.anteriorDe(etapa));
                garantirQueOPerfilAtende(etapa, recurso);
                yield StatusEtapaOrdemServico.DISPARADA;
            }
            // CA.6: a alocação direta também espera aceite — só que de uma pessoa só.
            // US-014 CA.1: pode ser aceita com antecedência; a ordem é cobrada na entrega.
            case ALOCADO -> {
                if (!etapa.pertenceA(recurso)) {
                    throw new AlocacaoInvalidaException("Esta etapa foi alocada a outro profissional");
                }
                yield StatusEtapaOrdemServico.ALOCADO;
            }
            default -> throw new TransicaoEtapaInvalidaException(
                    "A etapa " + etapa.getNome() + " não está disponível para aceite");
        };
    }

    /**
     * CA.4: só aceita quem poderia ter sido convidado. O convite vai por e-mail e o id da
     * etapa viaja em texto; sem esta conferência, um link repassado valeria como permissão.
     */
    private void garantirQueOPerfilAtende(EtapaOrdemServico etapa, Recurso recurso) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        boolean atende = recurso.atendePar(
                        solicitacao.getIdiomaOrigem().getId(),
                        solicitacao.getIdiomaDestino().getId())
                && recurso.executa(etapa.getTipoServico().getId());

        if (!atende) {
            throw new AlocacaoInvalidaException(
                    "Seu perfil não atende o par de idiomas ou o tipo de serviço desta etapa");
        }
    }
}
