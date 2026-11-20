package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.util.UUID;

/**
 * US-013 CA.1: o resultado do disparo.
 *
 * <p>Divergência do card: lá o exemplo usa {@code etapaId} e {@code recursosNotificados},
 * em camelCase. Aqui vai snake_case como no resto do módulo de ordens de serviço e no
 * portal do recurso — um controller que responde nas duas convenções obriga o front a
 * lembrar qual rota usa qual.
 */
public record DisparoEtapaResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        StatusEtapaOrdemServico status,
        @JsonProperty("recursos_notificados") int recursosNotificados
) {
}
