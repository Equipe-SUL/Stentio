package com.stentio.core.service;

import com.stentio.core.exception.OrcamentoIncompletoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.OrcamentoStatusInvalidoException;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.model.OrcamentoHistorico;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.OrcamentoEmailOutboxRepository;
import com.stentio.core.repository.OrcamentoHistoricoRepository;
import com.stentio.core.repository.OrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrcamentoEnvioServiceTest {
    private static final Instant AGORA = Instant.parse("2026-10-10T17:00:00Z");
    private final OrcamentoRepository repository = mock(OrcamentoRepository.class);
    private final OrcamentoHistoricoRepository historico = mock(OrcamentoHistoricoRepository.class);
    private final OrcamentoEmailOutboxRepository outbox = mock(OrcamentoEmailOutboxRepository.class);
    private final Cliente cliente = mock(Cliente.class);
    private Orcamento orcamento;
    private OrcamentoEnvioService service;

    @BeforeEach
    void preparar() {
        service = new OrcamentoEnvioService(repository, historico, outbox,
                JsonMapper.builder().build(), Clock.fixed(AGORA, ZoneOffset.UTC),
                java.time.Duration.ofDays(5), "https://stentio.example/orcamento");
        orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        orcamento.setCliente(cliente);
        orcamento.setStatus(StatusOrcamento.EM_ELABORACAO);
        orcamento.setValorTotal(new BigDecimal("150.00"));
        OrcamentoItemServico servico = new OrcamentoItemServico();
        servico.setTipoServico("Tradução");
        servico.setQuantidade(new BigDecimal("500"));
        servico.setValorUnitario(new BigDecimal("0.30"));
        servico.setSubtotal(new BigDecimal("150.00"));
        OrcamentoItem item = new OrcamentoItem();
        item.setOrdem(1);
        item.adicionarServico(servico);
        orcamento.adicionarItem(item);
        when(cliente.getEmailRepresentante()).thenReturn("cliente@example.com");
        when(cliente.getNomeRepresentante()).thenReturn("Maria Souza");
        when(cliente.getNomeEmpresa()).thenReturn("Empresa XPTO");
        when(repository.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));
    }

    private static int ocorrencias(String texto, String trecho) {
        return texto.split(java.util.regex.Pattern.quote(trecho), -1).length - 1;
    }

    @Test
    void enviaOrcamentoCompletoEGeraTokenEEmailNaOutbox() throws Exception {
        var response = service.enviarParaAprovacao(orcamento.getId());

        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, response.status());
        assertEquals(AGORA.plusSeconds(5 * 24 * 60 * 60), response.dataExpiracao());
        assertEquals(TokenOrcamento.hash(response.tokenAprovacao().toString()), orcamento.getTokenHash());
        assertEquals(AGORA, response.dataExpiracao().minusSeconds(5 * 24 * 60 * 60));
        assertEquals("Orçamento enviado para a fila de e-mails com sucesso", response.mensagem());

        var pending = org.mockito.ArgumentCaptor.forClass(OrcamentoEmailOutbox.class);
        verify(outbox).save(pending.capture());
        // US-13: a outbox guarda dados estruturados; o corpo é montado no email-service.
        var payload = JsonMapper.builder().build().readTree(pending.getValue().getPayload());
        assertEquals("cliente@example.com", payload.get("to").asText());
        assertEquals("Maria Souza", payload.get("cliente").get("nomeRepresentante").asText());
        assertEquals("Empresa XPTO", payload.get("cliente").get("nomeEmpresa").asText());

        var dadosOrcamento = payload.get("orcamento");
        String codigoEsperado = "ORC-2026-"
                + orcamento.getId().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
        assertEquals(codigoEsperado, dadosOrcamento.get("codigo").asText());
        assertEquals("Orçamento " + codigoEsperado + " aguardando sua aprovação",
                payload.get("subject").asText());
        assertEquals("150.00", dadosOrcamento.get("valorTotal").asText());
        assertEquals("https://stentio.example/orcamento?token=" + response.tokenAprovacao(),
                dadosOrcamento.get("linkAprovacao").asText());
        assertEquals(response.dataExpiracao().toString(), dadosOrcamento.get("dataExpiracao").asText());
        // O token não aparece mais solto: existe uma única vez no payload, dentro do link.
        assertEquals(1, ocorrencias(pending.getValue().getPayload(), response.tokenAprovacao().toString()));
        // Orçamento sem solicitação vinculada não quebra o envio.
        assertTrue(payload.get("solicitacao").isNull());
        assertTrue(payload.get("anexos").isEmpty());
        var audit = org.mockito.ArgumentCaptor.forClass(OrcamentoHistorico.class);
        verify(historico).save(audit.capture());
        assertEquals(StatusOrcamento.EM_ELABORACAO, audit.getValue().getStatusAnterior());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, audit.getValue().getStatusNovo());
        assertEquals("ATENDENTE", audit.getValue().getOrigem());
    }

    @Test
    void mensagemLevaDadosDaSolicitacaoEOsAnexosOrdenados() throws Exception {
        CategoriaProjeto categoria = new CategoriaProjeto();
        categoria.setNome("Jurídico");
        Idioma origem = new Idioma();
        origem.setId(UUID.randomUUID());
        origem.setNome("Português");
        Idioma destino = new Idioma();
        destino.setId(UUID.randomUUID());
        destino.setNome("Inglês");

        Solicitacao solicitacao = new Solicitacao(cliente, categoria, origem, destino, "Contrato social");
        solicitacao.anexarArquivo(new ArquivoArmazenado("contrato.pdf", "2026/10/a.pdf", "application/pdf", 10L));
        solicitacao.anexarArquivo(new ArquivoArmazenado("anexo.docx", "2026/10/b.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 20L));
        orcamento.setSolicitacao(solicitacao);

        service.enviarParaAprovacao(orcamento.getId());

        var pending = org.mockito.ArgumentCaptor.forClass(OrcamentoEmailOutbox.class);
        verify(outbox).save(pending.capture());
        var payload = JsonMapper.builder().build().readTree(pending.getValue().getPayload());

        var dados = payload.get("solicitacao");
        assertEquals("Jurídico", dados.get("categoria").asText());
        assertEquals("Português", dados.get("idiomaOrigem").asText());
        assertEquals("Inglês", dados.get("idiomaDestino").asText());
        assertEquals("Contrato social", dados.get("descricao").asText());
        assertEquals(solicitacao.getDataRecebimento().toString(), dados.get("dataRecebimento").asText());

        var anexos = payload.get("anexos");
        assertEquals(2, anexos.size());
        // Empate no enviadoEm cai no desempate por nome, então a ordem é estável entre envios.
        assertEquals("anexo.docx", anexos.get(0).get("nomeArquivo").asText());
        assertEquals("2026/10/b.docx", anexos.get(0).get("caminho").asText());
        assertEquals("contrato.pdf", anexos.get(1).get("nomeArquivo").asText());
        assertEquals("application/pdf", anexos.get(1).get("tipoConteudo").asText());
    }

    @Test
    void reenvioSubstituiTokenAnteriorEReativaTokenUsado() {
        String previousToken = TokenOrcamento.gerar().toString();
        orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
        orcamento.setTokenHash(TokenOrcamento.hash(previousToken));
        orcamento.setTokenUtilizadoEm(AGORA.minusSeconds(1));

        var response = service.enviarParaAprovacao(orcamento.getId());

        assertNotEquals(TokenOrcamento.hash(previousToken), orcamento.getTokenHash());
        assertEquals(TokenOrcamento.hash(response.tokenAprovacao().toString()), orcamento.getTokenHash());
        assertNull(orcamento.getTokenUtilizadoEm());
        verify(outbox).save(any(OrcamentoEmailOutbox.class));
    }

    @Test
    void semItensCalculadosRetornaOrcamentoIncompletoSemAlterarDados() {
        orcamento.getItens().clear();

        assertThrows(OrcamentoIncompletoException.class,
                () -> service.enviarParaAprovacao(orcamento.getId()));

        verify(repository, never()).save(any());
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void statusAprovadoNaoPodeSerEnviado() {
        orcamento.setStatus(StatusOrcamento.APROVADO);

        assertThrows(OrcamentoStatusInvalidoException.class,
                () -> service.enviarParaAprovacao(orcamento.getId()));

        verify(repository, never()).save(any());
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void orcamentoInexistenteGera404DomainException() {
        when(repository.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.empty());

        assertThrows(OrcamentoNaoEncontradoException.class,
                () -> service.enviarParaAprovacao(orcamento.getId()));

        verifyNoInteractions(historico, outbox);
    }
}
