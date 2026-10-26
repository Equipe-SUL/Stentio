package com.stentio.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrcamentoRabbitConfig {
    public static final String EXCHANGE = "stentio.orcamentos";
    public static final String QUEUE = "stentio.orcamentos.decisoes";
    public static final String EMAIL_QUEUE = "email.queue";
    /**
     * US-13: fila própria para o e-mail de orçamento. A email.queue continua recebendo o
     * {@code SendEmailRequest} genérico (assunto e corpo já prontos) usado pelo EmailController;
     * esta transporta dados estruturados, que o email-service renderiza. Separar evita que um
     * consumidor receba o payload do outro.
     */
    public static final String EMAIL_ORCAMENTO_QUEUE = "email.orcamento.queue";
    public static final String ORDEM_SERVICO_QUEUE = "stentio.ordens-servico.criacao";
    public static final String ORDEM_SERVICO_DLQ = "stentio.ordens-servico.criacao.dlq";

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Declarables orcamentoDeclarables() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        Queue emailQueue = new Queue(EMAIL_QUEUE, true);
        Queue emailOrcamentoQueue = new Queue(EMAIL_ORCAMENTO_QUEUE, true);
        Queue ordemServicoQueue = QueueBuilder.durable(ORDEM_SERVICO_QUEUE)
                .deadLetterExchange("").deadLetterRoutingKey(ORDEM_SERVICO_DLQ).build();
        Queue ordemServicoDlq = new Queue(ORDEM_SERVICO_DLQ, true);
        return new Declarables(exchange, queue, emailQueue, emailOrcamentoQueue,
                ordemServicoQueue, ordemServicoDlq,
                BindingBuilder.bind(ordemServicoQueue).to(exchange).with("orcamento.aprovado"),
                BindingBuilder.bind(queue).to(exchange).with("orcamento.aprovado"),
                BindingBuilder.bind(queue).to(exchange).with("orcamento.rejeitado"));
    }
}
