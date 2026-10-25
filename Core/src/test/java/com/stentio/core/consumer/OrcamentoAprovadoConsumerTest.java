package com.stentio.core.consumer;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.event.OrcamentoDecisaoEvento;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.service.OrdemServicoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoAprovadoConsumerTest {
    @Mock OrdemServicoService service;
    @InjectMocks OrcamentoAprovadoConsumer consumer;
    private final UUID orcamentoId = UUID.randomUUID();

    @Test
    void consomeJsonNoContratoPublicadoPelaUs14SemCabecalhoDeTipo() {
        String payload = """
                {"evento_id":"%s","versao_schema":1,"orcamento_id":"%s",
                 "status":"APROVADO","motivo":null,"data_decisao":"2026-10-09T08:00:00Z"}
                """.formatted(UUID.randomUUID(), orcamentoId);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(OrcamentoDecisaoEvento.class);
        var evento = (OrcamentoDecisaoEvento) new OrcamentoRabbitConfig().rabbitMessageConverter()
                .fromMessage(new Message(payload.getBytes(StandardCharsets.UTF_8), properties));

        consumer.consumir(evento);

        verify(service).criarParaOrcamentoAprovado(orcamentoId);
        assertEquals(Instant.parse("2026-10-09T08:00:00Z"), evento.dataDecisao());
    }

    @Test
    void recusaEventoInvalidoSemConsultarBanco() {
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(null));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(evento(2, orcamentoId, StatusOrcamento.APROVADO)));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(evento(1, null, StatusOrcamento.APROVADO)));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consumir(evento(1, orcamentoId, StatusOrcamento.REJEITADO)));
        verifyNoInteractions(service);
    }

    @Test
    void encaminhaFalhasDeNegocioParaFilaDeErros() {
        var falha = new OrcamentoInvalidoException("Orçamento não aprovado");
        when(service.criarParaOrcamentoAprovado(orcamentoId)).thenThrow(falha);

        var erro = assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.consumir(evento(1, orcamentoId, StatusOrcamento.APROVADO)));
        assertSame(falha, erro.getCause());
    }

    @Test
    void encaminhaOrcamentoInexistenteParaFilaDeErros() {
        when(service.criarParaOrcamentoAprovado(orcamentoId)).thenThrow(new OrcamentoNaoEncontradoException(orcamentoId));
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.consumir(evento(1, orcamentoId, StatusOrcamento.APROVADO)));
    }

    @Test
    void propagaFalhaTransitoriaParaPermitirReentrega() {
        var falha = new DataAccessResourceFailureException("Banco indisponível");
        when(service.criarParaOrcamentoAprovado(orcamentoId)).thenThrow(falha);

        assertSame(falha, assertThrows(DataAccessResourceFailureException.class,
                () -> consumer.consumir(evento(1, orcamentoId, StatusOrcamento.APROVADO))));
    }

    @Test
    void filaDuravelRecebeSomenteAprovacoesEPreservaMensagensRejeitadas() {
        var declarables = new OrcamentoRabbitConfig().orcamentoDeclarables();
        var bindings = declarables.getDeclarablesByType(Binding.class).stream()
                .filter(binding -> binding.getDestination().equals(OrcamentoRabbitConfig.ORDEM_SERVICO_QUEUE)).toList();
        assertEquals(1, bindings.size());
        assertEquals(OrcamentoRabbitConfig.EXCHANGE, bindings.getFirst().getExchange());
        assertEquals("orcamento.aprovado", bindings.getFirst().getRoutingKey());
        Queue queue = declarables.getDeclarablesByType(Queue.class).stream()
                .filter(item -> item.getName().equals(OrcamentoRabbitConfig.ORDEM_SERVICO_QUEUE)).findFirst().orElseThrow();
        assertTrue(queue.isDurable());
        assertEquals("", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(OrcamentoRabbitConfig.ORDEM_SERVICO_DLQ, queue.getArguments().get("x-dead-letter-routing-key"));
        assertTrue(declarables.getDeclarablesByType(Queue.class).stream()
                .anyMatch(item -> item.getName().equals(OrcamentoRabbitConfig.ORDEM_SERVICO_DLQ) && item.isDurable()));
    }

    private OrcamentoDecisaoEvento evento(int versao, UUID id, StatusOrcamento status) {
        return new OrcamentoDecisaoEvento(UUID.randomUUID(), versao, id, status, null, Instant.now());
    }
}
