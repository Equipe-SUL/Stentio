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
 * US-011: cadastro do modelo de workflow. O card deixou o Cenário 2 em branco, então este
 * contrato é proposta — espelha o da montagem manual de propósito, para quem aprendeu uma
 * tela já saber usar a outra.
 */
public record ModeloWorkflowRequest(
        @NotBlank(message = "Informe o nome do modelo")
        @Size(max = 120, message = "O nome do modelo deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

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
