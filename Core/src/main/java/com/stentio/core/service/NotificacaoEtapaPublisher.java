package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.NotificacaoEtapaOutbox;
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
 * US-013: põe a notificação na fila genérica de e-mail.
 *
 * <p>Vai para {@code email.queue}, que o email-service consome como SendEmailRequest, e
 * não para a fila do orçamento: a notificação é texto com um link, sem anexo nem montagem de
 * HTML, e não tem por que passar pelo renderizador daquele fluxo.
 */
@Component
public class NotificacaoEtapaPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long timeoutSegundos;

    public NotificacaoEtapaPublisher(RabbitTemplate rabbitTemplate,
                                 @Value("${app.orcamentos.outbox.confirm-timeout-segundos:5}") long timeoutSegundos) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutSegundos = timeoutSegundos;
    }

    public void publicar(NotificacaoEtapaOutbox notificacao) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(notificacao.getId().toString());

        Message message = new Message(notificacao.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlation = new CorrelationData(notificacao.getId() + ":" + UUID.randomUUID());
        rabbitTemplate.send("", OrcamentoRabbitConfig.EMAIL_QUEUE, message, correlation);

        // Sem esperar o confirm, "enviado" significaria apenas "entregamos ao cliente da
        // biblioteca", e um RabbitMQ fora do ar passaria por sucesso.
        CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutSegundos, TimeUnit.SECONDS);
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("Notificação não confirmada ou não roteado pelo RabbitMQ");
        }
    }
}
