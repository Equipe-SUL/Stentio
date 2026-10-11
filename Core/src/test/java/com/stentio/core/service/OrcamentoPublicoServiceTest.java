package com.stentio.core.service;

import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoPublicoServiceTest {
    @Mock OrcamentoRepository repository;
    @Mock OrcamentoHistoricoRepository historico;
    @Mock OrcamentoDecisaoOutboxRepository outbox;
    private final Instant agora = Instant.parse("2026-10-10T17:00:00Z");
    private final String token = "6bbd9908-4783-4991-a4f9-b0fc1807115a";
    private Orcamento orcamento;
    private OrcamentoPublicoService service;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void preparar() {
        service = new OrcamentoPublicoService(repository, historico, outbox, mapper,
                Clock.fixed(agora, ZoneOffset.UTC));
        orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
        orcamento.setDataExpiracao(agora.plusSeconds(3600));
        orcamento.setTokenHash(TokenOrcamento.hash(token));
        orcamento.setValorTotal(new BigDecimal("1500.00"));
        OrcamentoItem item = new OrcamentoItem();
        item.setTipoServico("Tradução Juramentada");
        item.setQuantidade(new BigDecimal("5000"));
        item.setUnidade("Palavras");
        item.setValorUnitario(new BigDecimal("0.30"));
        item.setSubtotal(new BigDecimal("1500.00"));
        orcamento.adicionarItem(item);
    }

    private void prepararDecisao() {
        when(repository.buscarPorTokenComLock(TokenOrcamento.hash(token))).thenReturn(Optional.of(orcamento));
    }

    @Test
    void consultaRetornaItensEValoresSemConsumirToken() {
        when(repository.findByTokenHash(TokenOrcamento.hash(token))).thenReturn(Optional.of(orcamento));
        var resposta = service.consultar(token);
        assertEquals(new BigDecimal("1500.00"), resposta.valorTotal());
        assertEquals("Tradução Juramentada", resposta.itens().getFirst().tipoServico());
        assertEquals(orcamento.getDataExpiracao(), resposta.dataExpiracao());
        service.consultar(token);
        assertNull(orcamento.getTokenUtilizadoEm());
        verify(repository, never()).save(any());
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void tokenInexistenteFalhaSemEfeitos() {
        prepararDecisao();
        when(repository.buscarPorTokenComLock(TokenOrcamento.hash(token))).thenReturn(Optional.empty());
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        verifyNoInteractions(historico, outbox);
        verify(repository, never()).save(any());
    }

    @Test
    void expiracaoExataJaEInvalida() {
        prepararDecisao();
        orcamento.setDataExpiracao(agora);
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void expiradoNaoAprovaNemGeraEvento() {
        prepararDecisao();
        orcamento.setDataExpiracao(agora.minusSeconds(1));
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, orcamento.getStatus());
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void aprovaGravaHistoricoConsomeTokenEEnfileiraEvento() {
        prepararDecisao();
        var resposta = service.aprovar(token);
        assertEquals(StatusOrcamento.APROVADO, resposta.status());
        assertEquals(agora, orcamento.getTokenUtilizadoEm());
        var registro = ArgumentCaptor.forClass(OrcamentoHistorico.class);
        verify(historico).save(registro.capture());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, registro.getValue().getStatusAnterior());
        assertEquals(StatusOrcamento.APROVADO, registro.getValue().getStatusNovo());
        assertNull(registro.getValue().getMotivo());
        var pendente = ArgumentCaptor.forClass(OrcamentoDecisaoOutbox.class);
        verify(outbox).save(pendente.capture());
        var evento = mapper.readTree(pendente.getValue().getPayload());
        assertEquals("orcamento.aprovado", pendente.getValue().getRoutingKey());
        assertEquals("APROVADO", evento.get("status").asText());
        assertEquals(pendente.getValue().getId().toString(), evento.get("evento_id").asText());
        assertFalse(pendente.getValue().getPayload().contains(token));
        assertNull(pendente.getValue().getPublicadoEm());
    }

    @Test
    void rejeitaGravaMotivoNoHistoricoEEvento() {
        prepararDecisao();
        assertEquals(StatusOrcamento.REJEITADO, service.rejeitar(token, "  Valor acima do orçamento  ").status());
        var registro = ArgumentCaptor.forClass(OrcamentoHistorico.class);
        verify(historico).save(registro.capture());
        assertEquals("Valor acima do orçamento", registro.getValue().getMotivo());
        var pendente = ArgumentCaptor.forClass(OrcamentoDecisaoOutbox.class);
        verify(outbox).save(pendente.capture());
        assertEquals("orcamento.rejeitado", pendente.getValue().getRoutingKey());
        assertEquals("Valor acima do orçamento", mapper.readTree(pendente.getValue().getPayload()).get("motivo").asText());
    }

    @Test
    void decisaoImpedeReusoParaAprovarRejeitarEConsultar() {
        prepararDecisao();
        when(repository.findByTokenHash(TokenOrcamento.hash(token))).thenReturn(Optional.of(orcamento));
        service.aprovar(token);
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.rejeitar(token, "Outro motivo"));
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.consultar(token));
        verify(historico, times(1)).save(any());
        verify(outbox, times(1)).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrcamento.class, names = {"RASCUNHO", "APROVADO", "REJEITADO"})
    void statusForaDeAguardandoImpedeDecisao(StatusOrcamento status) {
        prepararDecisao();
        orcamento.setStatus(status);
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void tokenUtilizadoImpedeDecisaoMesmoComStatusPendente() {
        prepararDecisao();
        orcamento.setTokenUtilizadoEm(agora.minusSeconds(10));
        assertThrows(LinkOrcamentoInvalidoException.class, () -> service.aprovar(token));
        verifyNoInteractions(historico, outbox);
    }

    @Test
    void motivoVazioOuLongoImpedeRejeicao() {
        assertThrows(IllegalArgumentException.class, () -> service.rejeitar(token, " "));
        assertThrows(IllegalArgumentException.class, () -> service.rejeitar(token, "a".repeat(2001)));
        verifyNoInteractions(repository, historico, outbox);
    }
}
