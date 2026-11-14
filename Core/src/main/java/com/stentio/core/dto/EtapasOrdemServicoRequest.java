package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * US-011 CA.3: a montagem manual envia a sequência inteira, não etapa a etapa. O gestor
 * adiciona, remove e reordena na tela e grava uma vez só — assim a validação da RN.1 vê
 * a sequência completa, que é a única forma de detectar lacuna.
 */
public record EtapasOrdemServicoRequest(
        @NotEmpty(message = "Informe ao menos uma etapa")
        @Valid
        List<Etapa> etapas
) {

    public record Etapa(
            @NotNull(message = "Informe a ordem da etapa")
            @Positive(message = "A ordem da etapa deve ser maior que zero")
            Integer ordem,

            @NotBlank(message = "Informe o nome da etapa")
            @Size(max = 120, message = "O nome da etapa deve ter no máximo 120 caracteres")
            String nome,

            // Chave do cadastro de tipos de serviço, não um texto fixo: o cliente administra
            // essa lista pela tela de Tipos de Serviço.
            @NotNull(message = "Informe o tipo de serviço da etapa")
            @JsonProperty("tipo_servico_id")
            UUID tipoServicoId,

            @NotNull(message = "Informe o prazo estimado em horas")
            @Positive(message = "O prazo estimado em horas deve ser maior que zero")
            @JsonProperty("prazo_estimado_horas")
            Integer prazoEstimadoHoras
    ) {
    }
}
