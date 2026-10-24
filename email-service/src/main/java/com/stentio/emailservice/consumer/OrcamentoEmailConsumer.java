package com.stentio.emailservice.consumer;

import com.stentio.emailservice.config.RabbitMQConfig;
import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.service.OrcamentoEmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * US-13: consome os dados do orçamento publicados pelo Core e dispara o e-mail de aprovação.
 *
 * <p>Fila separada da email.queue porque o payload é outro: aqui chegam dados estruturados,
 * lá chega um corpo de texto já pronto.
 */
@Component
public class OrcamentoEmailConsumer {

    private final OrcamentoEmailService orcamentoEmailService;

    public OrcamentoEmailConsumer(OrcamentoEmailService orcamentoEmailService) {
        this.orcamentoEmailService = orcamentoEmailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_ORCAMENTO_QUEUE)
    public void consumir(OrcamentoEmailMensagem mensagem) {
        orcamentoEmailService.enviar(mensagem);
    }
}
