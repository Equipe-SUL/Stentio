package com.stentio.core.service;

import com.stentio.core.dto.EntregaEtapaResponse;
import com.stentio.core.event.EventosAuditoria;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-013 CA.5 e RN.4: o profissional entrega o arquivo da etapa que assumiu.
 *
 * <p>Reaproveita o storage da solicitação: mesma pasta, mesma lista de extensões, mesma
 * proteção contra caminho fora da base. Um segundo mecanismo de upload seria uma segunda
 * superfície para revisar.
 */
@Service
public class EntregaEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final EtapaLiberacaoService liberacao;
    private final RecursoAutenticadoService recursoAutenticado;
    private final ArquivoStorageService storage;
    private final HistoricoEtapaRepository historico;
    private final RoteamentoEtapaService roteamento;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public EntregaEtapaService(EtapaOrdemServicoRepository etapas,
                               EtapaLiberacaoService liberacao,
                               RecursoAutenticadoService recursoAutenticado,
                               ArquivoStorageService storage,
                               HistoricoEtapaRepository historico,
                               RoteamentoEtapaService roteamento,
                               ApplicationEventPublisher eventos,
                               Clock clock) {
        this.etapas = etapas;
        this.liberacao = liberacao;
        this.recursoAutenticado = recursoAutenticado;
        this.storage = storage;
        this.historico = historico;
        this.roteamento = roteamento;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public EntregaEtapaResponse entregar(UUID etapaId, MultipartFile arquivo, UsuarioAutenticado ator) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new SolicitacaoInvalidaException("Envie o arquivo da entrega");
        }

        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        // RN.4 e CA.7: entrega quem assumiu, e só.
        if (!etapa.pertenceA(recurso)) {
            throw new AlocacaoInvalidaException("Esta etapa não está com você");
        }
        // A conferência de estado vem antes de gravar: recusar depois deixaria um arquivo
        // órfão no disco a cada tentativa em etapa já entregue.
        if (etapa.getStatus() != StatusEtapaOrdemServico.ACEITA) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + etapa.getNome() + " não está em execução para receber entrega");
        }
        boolean estaLiberada = liberacao.estaLiberada(etapa);
        if (!estaLiberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + etapa.getNome() + " aguarda a entrega da etapa anterior");
        }

        // US-015: numa reentrega depois de reprovação a etapa já tem arquivo, e a etapa
        // guarda um só. O caminho antigo é anotado antes de ser sobrescrito para o
        // arquivo sair do disco junto — senão cada correção deixa um órfão que nenhuma
        // linha do banco referencia.
        String caminhoSubstituido = etapa.getArquivoEntregueCaminho();

        Instant agora = clock.instant();
        String statusAnterior = EventosAuditoria.statusEtapa(etapa);
        ArquivoArmazenado salvo = storage.salvarTodos(List.of(arquivo)).getFirst();
        RoteamentoEtapaService.Resultado resultadoRoteamento;
        try {
            int atualizadas = etapas.entregar(etapaId, recurso, salvo.nomeOriginal(), salvo.caminho(),
                    agora, StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ENTREGUE);
            if (atualizadas != 1) {
                throw new TransicaoEtapaInvalidaException(
                        "A etapa " + etapa.getNome() + " não está mais em execução para receber entrega");
            }
            etapa = etapas.buscarParaAceite(etapaId)
                    .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));
            eventos.publishEvent(EventosAuditoria.etapa(etapa, statusAnterior,
                    "Arquivo " + salvo.nomeOriginal() + " entregue por " + recurso.getNome() + ".", null));
            resultadoRoteamento = roteamento.rotear(etapa);
            historico.save(new HistoricoEtapa(
                    etapa, EventoEtapa.ENTREGA, recurso, recurso.getEmail(), etapa.getEntregueEm()));
            historico.save(new HistoricoEtapa(
                    etapa, EventoEtapa.AVANCO, null, "sistema", etapa.getEntregueEm()));
            agendarLimpeza(salvo.caminho(), caminhoSubstituido);
        } catch (RuntimeException ex) {
            storage.remover(List.of(salvo.caminho()));
            throw ex;
        }

        EntregaEtapaResponse.Avanco avanco = resultadoRoteamento.proximaEtapa()
                .map(proxima -> new EntregaEtapaResponse.Avanco(
                        proxima.getId(),
                        proxima.getStatus(),
                        resultadoRoteamento.conviteAutomatico(),
                        resultadoRoteamento.recursosNotificados()))
                .orElse(null);

        return new EntregaEtapaResponse(
                etapa.getId(),
                etapa.getStatus(),
                etapa.getEntregueEm(),
                etapa.getArquivoEntregueNome(),
                "/api/v1/recurso/etapas/" + etapa.getId() + "/entrega/arquivo",
                avanco,
                new EntregaEtapaResponse.OrdemServico(
                        etapa.getOrdemServico().getId(),
                        etapa.getOrdemServico().getStatus()));
    }

    private void agendarLimpeza(String caminhoNovo, String caminhoSubstituido) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (caminhoSubstituido != null && !caminhoSubstituido.equals(caminhoNovo)) {
                    storage.remover(List.of(caminhoSubstituido));
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    storage.remover(List.of(caminhoNovo));
                }
            }
        });
    }

    /** O que o download precisa saber, já conferido. */
    @Transactional(readOnly = true)
    public ArquivoEntregue localizarEntrega(UUID ordemServicoId, UUID etapaId) {
        EtapaOrdemServico etapa = etapas.buscarParaAlocacao(ordemServicoId, etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        if (etapa.getArquivoEntregueCaminho() == null) {
            throw new SolicitacaoInvalidaException("Esta etapa ainda não tem arquivo entregue");
        }
        return new ArquivoEntregue(etapa.getArquivoEntregueNome(), etapa.getArquivoEntregueCaminho());
    }

    @Transactional(readOnly = true)
    public ArquivoEntregue localizarEntregaDoRecurso(UUID etapaId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .filter(candidata -> candidata.pertenceA(recurso))
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        if (etapa.getStatus() != StatusEtapaOrdemServico.ENTREGUE
                || etapa.getArquivoEntregueCaminho() == null) {
            throw new SolicitacaoInvalidaException("Esta etapa ainda não tem arquivo entregue");
        }
        return new ArquivoEntregue(etapa.getArquivoEntregueNome(), etapa.getArquivoEntregueCaminho());
    }

    public record ArquivoEntregue(String nomeOriginal, String caminho) {
    }
}
