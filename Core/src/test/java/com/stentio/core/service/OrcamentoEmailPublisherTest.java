package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.OrcamentoEmailOutbox;
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

class OrcamentoEmailPublisherTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final OrcamentoEmailPublisher publisher = new OrcamentoEmailPublisher(rabbit, 0);

    private OrcamentoEmailOutbox email() {
        var email = new OrcamentoEmailOutbox();
        email.setId(UUID.randomUUID());
        email.setPayload("{\"to\":\"cliente@example.com\",\"subject\":\"Orçamento\",\"body\":\"token\"}");
        return email;
    }

    private void confirmar(boolean ack, boolean retornada) {
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            if (retornada) {
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "", "email.queue"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void ackSemReturnPublicaJsonNaFilaDeEmail() throws Exception {
        confirmar(true, false);
        var email = email();

        publisher.publicar(email);

        verify(rabbit).send(eq(""), eq(OrcamentoRabbitConfig.EMAIL_QUEUE),
                argThat(message -> message.getMessageProperties().getMessageId().equals(email.getId().toString())
                        && message.getMessageProperties().getContentType().equals("application/json")
                        && java.util.Arrays.equals(message.getBody(), email.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                any(CorrelationData.class));
    }

    @Test
    void nackOuMensagemNaoRoteadaFalha() {
        confirmar(false, false);
        assertThrows(IllegalStateException.class, () -> publisher.publicar(email()));

        confirmar(true, true);
        assertThrows(IllegalStateException.class, () -> publisher.publicar(email()));
    }

    @Test
    void ausenciaDeConfirmacaoFalhaComTimeout() {
        assertThrows(TimeoutException.class, () -> publisher.publicar(email()));
    }
}
