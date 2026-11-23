package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusOrdemServico;

import java.util.UUID;

/**
 * US-015 CA.7: o aceite da aprovação, ainda não o encerramento.
 *
 * <p>Devolve 202 porque o trabalho continua depois da resposta: a OS só vira ENCERRADA
 * quando o broker confirmar a mensagem. A tela não deve dizer que o cliente recebeu
 * (RN.7) — o ACK confirma a entrega ao broker, não a leitura do e-mail.
 */
public record AprovacaoEntregaFinalResponse(
        @JsonProperty("outbox_id") UUID outboxId,
        @JsonProperty("idempotency_key") String idempotencyKey,
        StatusOrdemServico status
) {
}
