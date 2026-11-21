package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.EntregaFinalEmailOutbox;
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

/**
 * US-015 CA.7 e CA.8: põe o pacote na fila e espera o broker confirmar.
 *
 * <p>O messageId é a chave de idempotência, não o id da linha (CA.7): é por ela que o
 * consumidor reconhece um reenvio da mesma aprovação depois de uma falha de rede.
 */
@Component
public class EntregaFinalEmailPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long timeoutSegundos;

    public EntregaFinalEmailPublisher(RabbitTemplate rabbitTemplate,
            @Value("${app.orcamentos.outbox.confirm-timeout-segundos:5}") long timeoutSegundos) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutSegundos = timeoutSegundos;
    }

    public void publicar(EntregaFinalEmailOutbox envio) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(envio.getIdempotencyKey());

        Message message = new Message(envio.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlation = new CorrelationData(envio.getId() + ":" + UUID.randomUUID());

        rabbitTemplate.send("", OrcamentoRabbitConfig.EMAIL_ENTREGA_FINAL_QUEUE, message, correlation);

        CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutSegundos, TimeUnit.SECONDS);
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("Entrega final não confirmada ou não roteada pelo RabbitMQ");
        }
    }
}
