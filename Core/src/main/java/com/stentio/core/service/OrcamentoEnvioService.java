package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoAprovacaoResponse;
import com.stentio.core.exception.OrcamentoIncompletoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.OrcamentoStatusInvalidoException;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.model.OrcamentoHistorico;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.OrcamentoEmailOutboxRepository;
import com.stentio.core.repository.OrcamentoHistoricoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrcamentoEnvioService {
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
        email.setPayload(objectMapper.writeValueAsString(new EmailOrcamento(
                orcamento.getCliente().getEmailRepresentante(),
                "Orçamento aguardando sua aprovação",
                corpoEmail(orcamento, token))));
        email.setCriadoEm(agora);
        email.setProximaTentativaEm(agora);
        outbox.save(email);

        return new OrcamentoAprovacaoResponse(orcamento.getId(), orcamento.getStatus(),
                token, expiracao, MENSAGEM_SUCESSO);
    }

    private static void validarCompleto(Orcamento orcamento) {
        boolean statusPermitido = orcamento.getStatus() == StatusOrcamento.RASCUNHO
                || orcamento.getStatus() == StatusOrcamento.AGUARDANDO_APROVACAO
                || orcamento.getStatus() == StatusOrcamento.REJEITADO;
        boolean itensCalculados = orcamento.getItens() != null && !orcamento.getItens().isEmpty()
                && orcamento.getItens().stream().allMatch(item ->
                item.getQuantidade() != null && item.getQuantidade().signum() > 0
                        && item.getValorUnitario() != null && item.getValorUnitario().signum() > 0
                        && item.getSubtotal() != null && item.getSubtotal().signum() > 0);
        BigDecimal total = orcamento.getValorTotal();
        if (!statusPermitido || orcamento.getCliente() == null
                || orcamento.getCliente().getEmailRepresentante() == null
                || orcamento.getCliente().getEmailRepresentante().isBlank()
                || !itensCalculados || total == null || total.signum() <= 0) {
            throw new OrcamentoIncompletoException();
        }
    }

    private String corpoEmail(Orcamento orcamento, UUID token) {
        String separador = urlConsultaPublica.contains("?") ? "&" : "?";
        String link = urlConsultaPublica + separador + "token=" + token;
        return "Olá,\n\n"
                + "O orçamento " + orcamento.getId() + " está pronto para sua aprovação.\n"
                + "Valor total: " + orcamento.getValorTotal() + "\n"
                + "Acesse o orçamento pelo link: " + link + "\n"
                + "Token de aprovação: " + token + "\n"
                + "Este link expira em " + orcamento.getDataExpiracao() + ".";
    }

    private record EmailOrcamento(String to, String subject, String body) {
    }
}
