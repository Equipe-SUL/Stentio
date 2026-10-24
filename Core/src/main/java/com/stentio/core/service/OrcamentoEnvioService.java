package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoAprovacaoResponse;
import com.stentio.core.dto.OrcamentoEmailMensagem;
import com.stentio.core.exception.OrcamentoIncompletoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.OrcamentoStatusInvalidoException;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.model.OrcamentoHistorico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.OrcamentoEmailOutboxRepository;
import com.stentio.core.repository.OrcamentoHistoricoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.support.CodigoVisual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrcamentoEnvioService {
    private static final Logger log = LoggerFactory.getLogger(OrcamentoEnvioService.class);
    private static final String MENSAGEM_SUCESSO = "Orçamento enviado para a fila de e-mails com sucesso";

    private final OrcamentoRepository orcamentos;
    private final OrcamentoHistoricoRepository historico;
    private final OrcamentoEmailOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration validadeToken;
    private final String urlConsultaPublica;

    public OrcamentoEnvioService(OrcamentoRepository orcamentos,
                                 OrcamentoHistoricoRepository historico,
                                 OrcamentoEmailOutboxRepository outbox,
                                 ObjectMapper objectMapper,
                                 Clock clock,
                                 @Value("${app.orcamentos.token-validade:PT120H}") Duration validadeToken,
                                 @Value("${app.orcamentos.url-consulta-publica:http://localhost:8082/api/v1/public/orcamentos}")
                                 String urlConsultaPublica) {
        this.orcamentos = orcamentos;
        this.historico = historico;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.validadeToken = validadeToken;
        this.urlConsultaPublica = urlConsultaPublica;
        avisarSeUrlNaoForAlcancavelPeloCliente(urlConsultaPublica);
    }

    /**
     * O link vai para o e-mail de um cliente externo. Apontado para localhost, ele abre a
     * máquina de quem recebeu, não a nossa — e o orçamento nunca é aprovado. Em
     * desenvolvimento isso é esperado; em qualquer outro ambiente é configuração faltando,
     * então o aviso sai no boot e não só quando alguém reclama.
     */
    private static void avisarSeUrlNaoForAlcancavelPeloCliente(String url) {
        if (url != null && (url.contains("localhost") || url.contains("127.0.0.1"))) {
            log.warn("app.orcamentos.url-consulta-publica aponta para {} — o cliente externo não "
                    + "consegue abrir esse link. Defina ORCAMENTO_URL_CONSULTA_PUBLICA com o "
                    + "endereço público antes de enviar orçamentos de verdade.", url);
        }
    }

    @Transactional
    public OrcamentoAprovacaoResponse enviarParaAprovacao(UUID orcamentoId) {
        Orcamento orcamento = orcamentos.buscarPorIdComLock(orcamentoId)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(orcamentoId));
        if (orcamento.getStatus() == StatusOrcamento.APROVADO) {
            throw new OrcamentoStatusInvalidoException();
        }
        validarCompleto(orcamento);
        if (validadeToken == null || validadeToken.isZero() || validadeToken.isNegative()) {
            throw new IllegalStateException("A validade do token de aprovação deve ser positiva");
        }

        Instant agora = clock.instant();
        Instant expiracao = agora.plus(validadeToken);
        UUID token = TokenOrcamento.gerar();

        OrcamentoHistorico registro = new OrcamentoHistorico();
        registro.setOrcamento(orcamento);
        registro.setStatusAnterior(orcamento.getStatus());
        registro.setStatusNovo(StatusOrcamento.AGUARDANDO_APROVACAO);
        registro.setDataDecisao(agora);
        registro.setOrigem("ATENDENTE");

        orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
        orcamento.setTokenHash(TokenOrcamento.hash(token.toString()));
        orcamento.setTokenUtilizadoEm(null);
        orcamento.setDataExpiracao(expiracao);
        orcamentos.save(orcamento);
        historico.save(registro);

        UUID eventoId = UUID.randomUUID();
        OrcamentoEmailOutbox email = new OrcamentoEmailOutbox();
        email.setId(eventoId);
        email.setOrcamento(orcamento);
        email.setPayload(objectMapper.writeValueAsString(mensagemEmail(orcamento, token, agora)));
        email.setCriadoEm(agora);
        email.setProximaTentativaEm(agora);
        outbox.save(email);

        return new OrcamentoAprovacaoResponse(orcamento.getId(), orcamento.getStatus(),
                token, expiracao, MENSAGEM_SUCESSO);
    }

    private static void validarCompleto(Orcamento orcamento) {
        boolean statusPermitido = orcamento.getStatus() == StatusOrcamento.EM_ELABORACAO
                || orcamento.getStatus() == StatusOrcamento.AGUARDANDO_APROVACAO
                || orcamento.getStatus() == StatusOrcamento.REJEITADO;
        // US-11: cada item (par de idiomas) precisa ter ao menos um serviço, todos com valores calculados.
        boolean itensCalculados = orcamento.getItens() != null && !orcamento.getItens().isEmpty()
                && orcamento.getItens().stream().allMatch(item ->
                item.getServicos() != null && !item.getServicos().isEmpty()
                        && item.getServicos().stream().allMatch(servico ->
                        servico.getQuantidade() != null && servico.getQuantidade().signum() > 0
                                && servico.getValorUnitario() != null && servico.getValorUnitario().signum() > 0
                                && servico.getSubtotal() != null && servico.getSubtotal().signum() > 0));
        BigDecimal total = orcamento.getValorTotal();
        if (!statusPermitido || orcamento.getCliente() == null
                || orcamento.getCliente().getEmailRepresentante() == null
                || orcamento.getCliente().getEmailRepresentante().isBlank()
                || !itensCalculados || total == null || total.signum() <= 0) {
            throw new OrcamentoIncompletoException();
        }
    }

    /**
     * US-13: monta os dados que o email-service precisa para renderizar o corpo. Tudo é lido
     * aqui dentro, ainda na transação, porque solicitação e anexos são LAZY e a sessão fecha
     * ao sair do service (open-in-view está desligado).
     */
    private OrcamentoEmailMensagem mensagemEmail(Orcamento orcamento, UUID token, Instant agora) {
        Cliente cliente = orcamento.getCliente();
        // criadoEm só é preenchido no flush; no reenvio já existe, no primeiro envio cai em agora.
        Instant criacao = orcamento.getCriadoEm() != null ? orcamento.getCriadoEm() : agora;
        String codigo = CodigoVisual.gerar("ORC", criacao, orcamento.getId());
        Solicitacao solicitacao = orcamento.getSolicitacao();

        return new OrcamentoEmailMensagem(
                cliente.getEmailRepresentante(),
                "Orçamento " + codigo + " aguardando sua aprovação",
                new OrcamentoEmailMensagem.Cliente(cliente.getNomeRepresentante(), cliente.getNomeEmpresa()),
                new OrcamentoEmailMensagem.Orcamento(codigo, orcamento.getNumeroVersao(),
                        orcamento.getValorTotal(), linkAprovacao(token), texto(orcamento.getDataExpiracao())),
                dadosDaSolicitacao(solicitacao),
                anexosDa(solicitacao));
    }

    private String linkAprovacao(UUID token) {
        String separador = urlConsultaPublica.contains("?") ? "&" : "?";
        return urlConsultaPublica + separador + "token=" + token;
    }

    private static OrcamentoEmailMensagem.Solicitacao dadosDaSolicitacao(Solicitacao solicitacao) {
        if (solicitacao == null) {
            return null;
        }
        return new OrcamentoEmailMensagem.Solicitacao(
                solicitacao.getCategoriaProjeto().getNome(),
                solicitacao.getIdiomaOrigem().getNome(),
                solicitacao.getIdiomaDestino().getNome(),
                solicitacao.getDescricao(),
                texto(solicitacao.getDataRecebimento()));
    }

    private static List<OrcamentoEmailMensagem.Anexo> anexosDa(Solicitacao solicitacao) {
        if (solicitacao == null || solicitacao.getArquivos() == null) {
            return List.of();
        }
        return solicitacao.getArquivos().stream()
                // Mesma ordem estável da US-09, para os anexos não trocarem de posição entre envios.
                .sorted(Comparator.comparing(ArquivoSolicitacao::getEnviadoEm)
                        .thenComparing(ArquivoSolicitacao::getNomeOriginal))
                .map(arquivo -> new OrcamentoEmailMensagem.Anexo(
                        arquivo.getNomeOriginal(), arquivo.getCaminho(), arquivo.getTipoConteudo()))
                .toList();
    }

    private static String texto(Temporal data) {
        return data == null ? null : data.toString();
    }
}
