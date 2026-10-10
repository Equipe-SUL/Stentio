package com.stentio.core.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrcamentoRabbitConfig {
    public static final String EXCHANGE = "stentio.orcamentos";
    public static final String QUEUE = "stentio.orcamentos.decisoes";

    @Bean
    public Declarables orcamentoDeclarables() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        return new Declarables(exchange, queue,
                BindingBuilder.bind(queue).to(exchange).with("orcamento.aprovado"),
                BindingBuilder.bind(queue).to(exchange).with("orcamento.rejeitado"));
    }
}
