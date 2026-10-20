package com.stentio.core.consumer;

import com.stentio.core.config.OrcamentoRabbitConfig;
import com.stentio.core.event.OrcamentoDecisaoEvento;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.service.OrdemServicoService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoAprovadoConsumer {
    private final OrdemServicoService service;

    public OrcamentoAprovadoConsumer(OrdemServicoService service) {
        this.service = service;
    }

    @RabbitListener(queues = OrcamentoRabbitConfig.ORDEM_SERVICO_QUEUE)
    public void consumir(OrcamentoDecisaoEvento evento) {
        if (evento == null || evento.versaoSchema() != 1 || evento.orcamentoId() == null
                || evento.status() != StatusOrcamento.APROVADO) {
            throw new AmqpRejectAndDontRequeueException("Evento de aprovação de orçamento inválido");
        }
        try {
            service.criarParaOrcamentoAprovado(evento.orcamentoId());
        } catch (OrcamentoInvalidoException | OrcamentoNaoEncontradoException ex) {
            // Preserva falhas de negócio na DLQ; falhas de infraestrutura continuam permitindo reentrega.
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }
}
