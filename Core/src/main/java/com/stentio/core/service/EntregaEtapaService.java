package com.stentio.core.service;

import com.stentio.core.dto.EntregaEtapaResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
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
    private final Clock clock;

    public EntregaEtapaService(EtapaOrdemServicoRepository etapas,
                               EtapaLiberacaoService liberacao,
                               RecursoAutenticadoService recursoAutenticado,
                               ArquivoStorageService storage,
                               HistoricoEtapaRepository historico,
                               Clock clock) {
        this.etapas = etapas;
        this.liberacao = liberacao;
        this.recursoAutenticado = recursoAutenticado;
        this.storage = storage;
        this.historico = historico;
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

        ArquivoArmazenado salvo = storage.salvarTodos(List.of(arquivo)).getFirst();
        try {
            etapa.entregar(salvo.nomeOriginal(), salvo.caminho(), clock.instant(), estaLiberada);
            etapas.save(etapa);
        } catch (RuntimeException ex) {
            // O arquivo já está no disco e o banco não vai registrá-lo: apaga agora, senão
            // fica ocupando espaço sem ninguém que saiba que ele existe.
            storage.remover(List.of(salvo.caminho()));
            throw ex;
        }

        // CA.8
        historico.save(new HistoricoEtapa(
                etapa, EventoEtapa.ENTREGA, recurso, ator.email(), etapa.getEntregueEm()));

        return new EntregaEtapaResponse(
                etapa.getId(),
                etapa.getStatus(),
                etapa.getEntregueEm(),
                etapa.getArquivoEntregueNome());
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

    public record ArquivoEntregue(String nomeOriginal, String caminho) {
    }
}
