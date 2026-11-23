package com.stentio.core.service;

import com.stentio.core.dto.AprovacaoEntregaFinalResponse;
import com.stentio.core.dto.EntregaFinalEmailMensagem;
import com.stentio.core.exception.AnexoAcimaDoLimiteException;
import com.stentio.core.exception.ArquivoEntregueIndisponivelException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.EntregaFinalEmailOutbox;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.CodigoVisual;
import com.stentio.core.support.TipoConteudoArquivo;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * US-015 CA.5 a CA.9: o gestor aprova e o pacote entra na fila para o cliente.
 *
 * <p>Nada é publicado aqui. A transação grava a intenção na outbox e termina; quem publica
 * é o dispatcher, e é o ACK dele que encerra a OS. Assim não existe OS encerrada cuja
 * mensagem nunca saiu, nem mensagem de uma aprovação que acabou revertida.
 */
@Service
public class AprovacaoEntregaFinalService {

    private final EntregaFinalService revisao;
    private final EntregaFinalEmailOutboxRepository outbox;
    private final HistoricoEtapaRepository historico;
    private final ArquivoStorageService storage;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final long limiteTotalBytes;

    public AprovacaoEntregaFinalService(EntregaFinalService revisao,
                                        EntregaFinalEmailOutboxRepository outbox,
                                        HistoricoEtapaRepository historico,
                                        ArquivoStorageService storage,
                                        ObjectMapper objectMapper,
                                        Clock clock,
                                        @Value("${app.anexos.limite-total-bytes:10485760}") long limiteTotalBytes) {
        this.revisao = revisao;
        this.outbox = outbox;
        this.historico = historico;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.limiteTotalBytes = limiteTotalBytes;
    }

    @Transactional
    public AprovacaoEntregaFinalResponse aprovar(UUID ordemServicoId, String mensagem,
                                                 UsuarioAutenticado ator) {
        OrdemServico ordem = revisao.buscarParaAtualizacao(ordemServicoId);
        revisao.exigirAguardandoVerificacao(ordem);

        EtapaOrdemServico ultima = ultimaEntregue(ordemServicoId);
        String chave = chaveDe(ordemServicoId, ultima);

        // CA.9 e Cenário 6: aprovar de novo devolve a mesma mensagem em vez de criar outra.
        // A conferência é por chave, não por "existe pendente": depois de uma reabertura e
        // nova entrega a chave muda, e aí o reenvio é legítimo (RN.6).
        Optional<EntregaFinalEmailOutbox> jaExiste = outbox.findByIdempotencyKey(chave);
        if (jaExiste.isPresent()) {
            return resposta(jaExiste.get(), ordem);
        }

        long tamanho = medirAnexo(ultima);
        if (tamanho > limiteTotalBytes) {
            // CA.6: 413 antes de gravar qualquer coisa. Não existe envio parcial.
            throw new AnexoAcimaDoLimiteException(tamanho, limiteTotalBytes);
        }

        Instant agora = clock.instant();
        EntregaFinalEmailOutbox pendente = new EntregaFinalEmailOutbox(
                ordem, chave, ator.email(),
                objectMapper.writeValueAsString(montarMensagem(ordem, ultima, chave, tamanho, mensagem)),
                agora);
        outbox.save(pendente);

        // CA.7: o rastro entra na mesma transação da outbox.
        historico.save(new HistoricoEtapa(
                ultima, EventoEtapa.APROVACAO_FINAL, ultima.getRecurso(), ator.email(), agora));

        return resposta(pendente, ordem);
    }

    /** RN.3: o cliente recebe o arquivo da última etapa entregue. */
    private EtapaOrdemServico ultimaEntregue(UUID ordemServicoId) {
        List<EtapaOrdemServico> entregues = revisao.entreguesEmOrdem(ordemServicoId);
        if (entregues.isEmpty()) {
            throw new ArquivoEntregueIndisponivelException(
                    "Nenhuma etapa desta ordem de serviço foi entregue");
        }
        EtapaOrdemServico ultima = entregues.getLast();
        if (ultima.getArquivoEntregueCaminho() == null) {
            throw new ArquivoEntregueIndisponivelException(
                    "A última etapa entregue não tem arquivo para enviar");
        }
        return ultima;
    }

    /**
     * CA.6: o arquivo precisa existir e ser legível agora, não quando foi gravado.
     *
     * <p>Entre a entrega e a aprovação pode ter havido limpeza de volume ou restauração de
     * backup. Descobrir isso aqui é um 422 para o gestor; descobrir no dispatcher seria uma
     * OS presa esperando um envio que nunca vai conseguir acontecer.
     */
    private long medirAnexo(EtapaOrdemServico ultima) {
        try {
            return storage.carregar(ultima.getArquivoEntregueCaminho()).contentLength();
        } catch (Exception ex) {
            throw new ArquivoEntregueIndisponivelException(
                    "O arquivo entregue na etapa " + ultima.getNome() + " não pôde ser lido");
        }
    }

    /**
     * RN.6: a chave muda a cada nova entrega da última etapa.
     *
     * <p>É isso que separa "aprovou duas vezes" de "aprovou de novo depois de uma
     * correção": o primeiro caso repete a chave e não cria nada; o segundo tem entregue_em
     * diferente e gera um envio legítimo.
     */
    private static String chaveDe(UUID ordemServicoId, EtapaOrdemServico ultima) {
        return "ENTREGA_FINAL:" + ordemServicoId + ":" + ultima.getEntregueEm();
    }

    private EntregaFinalEmailMensagem montarMensagem(OrdemServico ordem, EtapaOrdemServico ultima,
                                                     String chave, long tamanho, String mensagem) {
        Cliente cliente = ordem.getOrcamento().getCliente();
        String codigo = CodigoVisual.gerar("OS", ordem.getCriadoEm(), ordem.getId());

        return new EntregaFinalEmailMensagem(
                EntregaFinalEmailMensagem.SCHEMA_ATUAL,
                chave,
                cliente.getEmailRepresentante(),
                "Documentos traduzidos da OS " + codigo,
                new EntregaFinalEmailMensagem.Cliente(
                        cliente.getNomeRepresentante(), cliente.getNomeEmpresa()),
                new EntregaFinalEmailMensagem.OrdemServico(ordem.getId().toString(), codigo),
                new EntregaFinalEmailMensagem.Anexo(
                        ultima.getId().toString(),
                        ultima.getArquivoEntregueNome(),
                        ultima.getArquivoEntregueCaminho(),
                        TipoConteudoArquivo.de(ultima.getArquivoEntregueNome()),
                        tamanho),
                mensagem);
    }

    private static AprovacaoEntregaFinalResponse resposta(EntregaFinalEmailOutbox pendente,
                                                          OrdemServico ordem) {
        return new AprovacaoEntregaFinalResponse(
                pendente.getId(), pendente.getIdempotencyKey(), ordem.getStatus());
    }
}
