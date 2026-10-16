package com.stentio.core.service;

import com.stentio.core.exception.OrcamentoIncompletoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.OrcamentoStatusInvalidoException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.model.OrcamentoHistorico;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
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
        when(repository.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));
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
        var payload = JsonMapper.builder().build().readTree(pending.getValue().getPayload());
        assertEquals("cliente@example.com", payload.get("to").asText());
        assertTrue(payload.get("body").asText().contains(orcamento.getId().toString()));
        assertTrue(payload.get("body").asText().contains(response.tokenAprovacao().toString()));
        assertTrue(payload.get("body").asText().contains("https://stentio.example/orcamento?token="));
        var audit = org.mockito.ArgumentCaptor.forClass(OrcamentoHistorico.class);
        verify(historico).save(audit.capture());
        assertEquals(StatusOrcamento.EM_ELABORACAO, audit.getValue().getStatusAnterior());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, audit.getValue().getStatusNovo());
        assertEquals("ATENDENTE", audit.getValue().getOrigem());
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
