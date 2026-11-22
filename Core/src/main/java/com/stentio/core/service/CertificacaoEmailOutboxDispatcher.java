package com.stentio.core.service;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.model.CertificacaoEmailOutbox;
import com.stentio.core.repository.CertificacaoEmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** US-024 CA.5: tira um aviso da outbox e publica na email.queue, com confirmação. */
@Service
public class CertificacaoEmailOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CertificacaoEmailOutboxDispatcher.class);

    private final CertificacaoEmailOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final long retrySegundos;
    private final long timeoutSegundos;

    public CertificacaoEmailOutboxDispatcher(CertificacaoEmailOutboxRepository repository,
            RabbitTemplate rabbitTemplate, Clock clock,
            @Value("${app.orcamentos.outbox.retry-segundos:30}") long retrySegundos,
            @Value("${app.orcamentos.outbox.confirm-timeout-segundos:5}") long timeoutSegundos) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.retrySegundos = retrySegundos;
        this.timeoutSegundos = timeoutSegundos;
    }

    @Transactional
    public boolean processarProximo() {
        CertificacaoEmailOutbox aviso = repository.buscarPendenteComLock(clock.instant()).orElse(null);
        if (aviso == null) {
            return false;
        }

        aviso.setTentativas(aviso.getTentativas() + 1);
        try {
            publicar(aviso);
            aviso.setEnviadoEm(clock.instant());
            aviso.setPayload(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            aviso.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            return false;
        } catch (Exception ex) {
            aviso.setProximaTentativaEm(clock.instant().plusSeconds(retrySegundos));
            log.warn("Aviso de rejeição da certificação {} pendente; nova tentativa agendada ({})",
                    aviso.getCertificacao().getId(), ex.getClass().getSimpleName());
        }
        return true;
    }

    private void publicar(CertificacaoEmailOutbox aviso) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(aviso.getId().toString());

        Message message = new Message(aviso.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlation = new CorrelationData(aviso.getId() + ":" + UUID.randomUUID());

        rabbitTemplate.send("", OrcamentoRabbitConfig.EMAIL_QUEUE, message, correlation);

        CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutSegundos, TimeUnit.SECONDS);
        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("Aviso de certificação não confirmado pelo RabbitMQ");
        }
    }
}
