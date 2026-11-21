package com.stentio.emailservice.consumer;

import com.stentio.emailservice.config.RabbitMQConfig;
import com.stentio.emailservice.dto.EntregaFinalEmailMensagem;
import com.stentio.emailservice.service.EntregaFinalEmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * US-015: consome a entrega final e manda o documento ao cliente.
 *
 * <p>Fila própria, e não a email.queue genérica: aquela leva assunto e corpo já prontos e
 * não tem onde colocar um anexo.
 */
@Component
public class EntregaFinalEmailConsumer {

    private final EntregaFinalEmailService entregaFinalEmailService;

    public EntregaFinalEmailConsumer(EntregaFinalEmailService entregaFinalEmailService) {
        this.entregaFinalEmailService = entregaFinalEmailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_ENTREGA_FINAL_QUEUE)
    public void consumir(EntregaFinalEmailMensagem mensagem) {
        entregaFinalEmailService.enviar(mensagem);
    }
}
