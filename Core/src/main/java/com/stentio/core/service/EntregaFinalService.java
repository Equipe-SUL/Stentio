package com.stentio.core.service;

import com.stentio.core.dto.EntregaFinalResponse;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.exception.TransicaoOrdemServicoInvalidaException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import com.stentio.core.support.CodigoVisual;
import com.stentio.core.support.TipoConteudoArquivo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * US-015 CA.2 e CA.3: a conferência do gestor antes de mandar para o cliente.
 *
 * <p>A revisão mostra tudo o que foi entregue, mas o pacote é só a última etapa (RN.3): as
 * anteriores são insumo interno — a tradução que o revisor corrigiu não interessa a quem
 * encomendou o serviço.
 */
@Service
public class EntregaFinalService {

    private final OrdemServicoRepository ordens;
    private final EtapaOrdemServicoRepository etapas;
    private final ArquivoStorageService storage;
    private final EntregaFinalEmailOutboxRepository outbox;
    private final long limiteTotalBytes;

    public EntregaFinalService(OrdemServicoRepository ordens,
                               EtapaOrdemServicoRepository etapas,
                               ArquivoStorageService storage,
                               EntregaFinalEmailOutboxRepository outbox,
                               @Value("${app.anexos.limite-total-bytes:10485760}") long limiteTotalBytes) {
        this.ordens = ordens;
        this.etapas = etapas;
        this.storage = storage;
        this.outbox = outbox;
        this.limiteTotalBytes = limiteTotalBytes;
    }

    @Transactional(readOnly = true)
    public EntregaFinalResponse revisar(UUID ordemServicoId) {
        OrdemServico ordem = buscarParaRevisao(ordemServicoId);
        exigirAguardandoVerificacao(ordem);

        List<EtapaOrdemServico> entregues = entreguesEmOrdem(ordemServicoId);
        EtapaOrdemServico ultima = entregues.isEmpty() ? null : entregues.getLast();

        Cliente cliente = ordem.getOrcamento().getCliente();

        return new EntregaFinalResponse(
                ordem.getId(),
                CodigoVisual.gerar("OS", ordem.getCriadoEm(), ordem.getId()),
                ordem.getStatus(),
                ordem.getMotivoRevisao(),
                new EntregaFinalResponse.Cliente(
                        cliente.getId(), cliente.getNomeEmpresa(), cliente.getEmailRepresentante()),
                entregues.stream().map(etapa -> paraItem(ordemServicoId, etapa)).toList(),
                montarPacote(ordemServicoId, ultima));
    }

    /** CA.2: fora de AGUARDANDO_VERIFICACAO não há o que revisar. */
    public void exigirAguardandoVerificacao(OrdemServico ordem) {
        if (ordem.getStatus() != StatusOrdemServico.AGUARDANDO_VERIFICACAO) {
            throw new TransicaoOrdemServicoInvalidaException(
                    "A ordem de serviço não está aguardando verificação");
        }
    }

    public OrdemServico buscarParaRevisao(UUID ordemServicoId) {
        return ordens.buscarComCliente(ordemServicoId)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(ordemServicoId));
    }

    public OrdemServico buscarParaAtualizacao(UUID ordemServicoId) {
        return ordens.buscarComClienteParaAtualizar(ordemServicoId)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(ordemServicoId));
    }

    /** CA.3: todas as entregues, da primeira para a última. */
    public List<EtapaOrdemServico> entreguesEmOrdem(UUID ordemServicoId) {
        return etapas.listarPorOrdemServico(ordemServicoId).stream()
                .filter(etapa -> etapa.getStatus() == StatusEtapaOrdemServico.ENTREGUE)
                .sorted(Comparator.comparingInt(EtapaOrdemServico::getOrdem))
                .toList();
    }

    private static EntregaFinalResponse.Etapa paraItem(UUID ordemServicoId, EtapaOrdemServico etapa) {
        return new EntregaFinalResponse.Etapa(
                etapa.getId(),
                etapa.getNome(),
                etapa.getOrdem(),
                etapa.getStatus(),
                etapa.getEntregueEm(),
                etapa.getArquivoEntregueNome(),
                "/api/v1/ordens-servico/" + ordemServicoId + "/etapas/" + etapa.getId() + "/arquivo");
    }

    /**
     * RN.4: por que o envio não pode sair, em vez de um botão desabilitado sem explicação.
     *
     * <p>Os bloqueios são acumulados, não interrompidos no primeiro: o gestor resolve tudo
     * de uma vez em vez de descobrir um problema por tentativa.
     */
    private EntregaFinalResponse.Pacote montarPacote(UUID ordemServicoId, EtapaOrdemServico ultima) {
        List<String> bloqueios = new ArrayList<>();

        if (ultima == null || ultima.getArquivoEntregueCaminho() == null) {
            bloqueios.add("A última etapa não tem arquivo entregue");
            return new EntregaFinalResponse.Pacote(
                    ultima == null ? null : ultima.getId(), null, null, null, false, bloqueios);
        }

        Long tamanho = null;
        try {
            tamanho = storage.carregar(ultima.getArquivoEntregueCaminho()).contentLength();
        } catch (Exception ex) {
            bloqueios.add("O arquivo entregue não pôde ser lido no armazenamento");
        }

        if (tamanho != null && tamanho > limiteTotalBytes) {
            bloqueios.add("O arquivo tem " + tamanho + " bytes e o limite de envio é " + limiteTotalBytes);
        }
        if (outbox.existsByOrdemServicoIdAndEnviadoEmIsNull(ordemServicoId)) {
            bloqueios.add("Já existe um envio desta ordem de serviço aguardando confirmação");
        }

        return new EntregaFinalResponse.Pacote(
                ultima.getId(),
                ultima.getArquivoEntregueNome(),
                TipoConteudoArquivo.de(ultima.getArquivoEntregueNome()),
                tamanho,
                bloqueios.isEmpty(),
                bloqueios);
    }
}
