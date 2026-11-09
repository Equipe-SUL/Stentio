package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

// US-012 CA.3 e CA.4: o corpo do POST de alocação, no formato do contrato do card.
public record AlocacaoEtapaRequest(
        @NotNull(message = "Informe o recurso a alocar")
        @JsonProperty("recurso_id")
        UUID recursoId,

        // Que a data esteja no futuro é regra de negócio, e quem cobra é a entidade.
        @NotNull(message = "Informe a data limite de entrega")
        @JsonProperty("data_limite_entrega")
        Instant dataLimiteEntrega
) {
}
