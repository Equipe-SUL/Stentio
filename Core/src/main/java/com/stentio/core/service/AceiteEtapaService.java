package com.stentio.core.service;

import com.stentio.core.dto.AceiteEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
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
    private final Clock clock;

    public AceiteEtapaService(EtapaOrdemServicoRepository etapas,
                              RecursoAutenticadoService recursoAutenticado,
                              Clock clock) {
        this.etapas = etapas;
        this.recursoAutenticado = recursoAutenticado;
        this.clock = clock;
    }

    @Transactional
    public AceiteEtapaResponse aceitar(UUID etapaId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        garantirQuePodeAceitar(etapa, recurso);

        // CA.3: o status esperado entra na condição do UPDATE. Quem perder a corrida
        // atualiza zero linhas, e zero linhas é a resposta 409 — não um aceite silencioso
        // por cima do colega.
        int atualizadas = etapas.aceitarSeDisponivel(
                etapaId,
                recurso,
                clock.instant(),
                StatusEtapaOrdemServico.ACEITA,
                StatusEtapaOrdemServico.DISPARADA);

        if (atualizadas == 0) {
            throw new TransicaoEtapaInvalidaException("Esta etapa já foi aceita por outro profissional");
        }

        // O UPDATE em massa limpa o contexto de persistência, então a etapa em memória
        // está velha: a resposta vem de uma leitura nova.
        EtapaOrdemServico aceita = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        return new AceiteEtapaResponse(
                aceita.getId(),
                aceita.getStatus(),
                aceita.getAceitoEm(),
                aceita.getDataLimiteEntrega());
    }

    /**
     * CA.4: só aceita quem poderia ter sido convidado. O convite vai por e-mail e o id da
     * etapa viaja em texto; sem esta conferência, um link repassado valeria como permissão.
     */
    private void garantirQuePodeAceitar(EtapaOrdemServico etapa, Recurso recurso) {
        if (!recurso.elegivelParaAlocacao()) {
            throw new AlocacaoInvalidaException(
                    "Seu cadastro ainda não foi verificado pela equipe da Aliança");
        }

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
