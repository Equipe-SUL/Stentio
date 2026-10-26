package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.OrcamentoDecisaoOutbox;
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
public class OrcamentoEventoPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final long timeoutSegundos;

    public OrcamentoEventoPublisher(RabbitTemplate rabbitTemplate,
            @Value("${app.orcamentos.outbox.confirm-timeout-segundos:5}") long timeoutSegundos) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutSegundos = timeoutSegundos;
    }

    public void publicar(OrcamentoDecisaoOutbox evento) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(evento.getId().toString());
        Message message = new Message(evento.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        // A tentativa tem correlação única; o messageId do evento continua estável.
        CorrelationData correlation = new CorrelationData(evento.getId() + ":" + UUID.randomUUID());
        rabbitTemplate.send(OrcamentoRabbitConfig.EXCHANGE, evento.getRoutingKey(), message, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutSegundos, TimeUnit.SECONDS);
        // ACK sozinho não garante roteamento: mandatory + returns também são verificados.
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("Evento não confirmado ou não roteado pelo RabbitMQ");
        }
    }
}
