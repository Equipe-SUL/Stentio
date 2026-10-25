package com.stentio.emailservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_QUEUE = "email.queue";
    // US-13: fila do e-mail de orçamento, que trafega dados estruturados em vez de corpo pronto.
    public static final String EMAIL_ORCAMENTO_QUEUE = "email.orcamento.queue";

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue emailOrcamentoQueue() {
        return new Queue(EMAIL_ORCAMENTO_QUEUE, true);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

}