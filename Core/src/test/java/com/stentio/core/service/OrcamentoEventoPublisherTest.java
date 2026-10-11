package com.stentio.core.service;

import com.stentio.core.model.OrcamentoDecisaoOutbox;
import com.stentio.core.config.OrcamentoRabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrcamentoEventoPublisherTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final OrcamentoEventoPublisher publisher = new OrcamentoEventoPublisher(rabbit, 0);

    private OrcamentoDecisaoOutbox evento() {
        var evento = new OrcamentoDecisaoOutbox();
        evento.setId(UUID.randomUUID());
        evento.setPayload("{\"status\":\"APROVADO\"}");
        evento.setRoutingKey("orcamento.aprovado");
        return evento;
    }

    private void confirmar(boolean ack, boolean retornada) {
        doAnswer(invocation -> {
            Message msg = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            if (retornada) correlation.setReturned(new ReturnedMessage(msg, 312, "NO_ROUTE",
                    OrcamentoRabbitConfig.EXCHANGE, "orcamento.aprovado"));
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void ackSemReturnConfirma() throws Exception {
        confirmar(true, false);
        var evento = evento();
        publisher.publicar(evento);
        verify(rabbit).send(eq(OrcamentoRabbitConfig.EXCHANGE), eq("orcamento.aprovado"),
                argThat(m -> evento.getId().toString().equals(m.getMessageProperties().getMessageId())),
                any(CorrelationData.class));
    }

    @Test
    void ackComReturnNaoEConsideradoEntregue() {
        confirmar(true, true);
        assertThrows(IllegalStateException.class, () -> publisher.publicar(evento()));
    }

    @Test
    void nackNaoConfirma() {
        confirmar(false, false);
        assertThrows(IllegalStateException.class, () -> publisher.publicar(evento()));
    }

    @Test
    void ausenciaDeConfirmacaoGeraTimeout() {
        assertThrows(TimeoutException.class, () -> publisher.publicar(evento()));
    }
}
