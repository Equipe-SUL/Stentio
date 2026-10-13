package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.OrcamentoEmailOutbox;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OrcamentoEmailPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final long timeoutSegundos;

    public OrcamentoEmailPublisher(RabbitTemplate rabbitTemplate,
            @Value("${app.orcamentos.outbox.confirm-timeout-segundos:5}") long timeoutSegundos) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutSegundos = timeoutSegundos;
    }

    public void publicar(OrcamentoEmailOutbox email) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(email.getId().toString());
        Message message = new Message(email.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlation = new CorrelationData(email.getId() + ":" + UUID.randomUUID());
        rabbitTemplate.send("", OrcamentoRabbitConfig.EMAIL_QUEUE, message, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutSegundos, TimeUnit.SECONDS);
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("E-mail não confirmado ou não roteado pelo RabbitMQ");
        }
    }
}
