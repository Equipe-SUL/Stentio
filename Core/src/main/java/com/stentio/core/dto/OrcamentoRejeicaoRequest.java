package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrcamentoRejeicaoRequest(
        @NotBlank String token,
        @NotBlank(message = "Motivo da rejeição é obrigatório")
        @Size(max = 2000, message = "Motivo deve ter no máximo 2000 caracteres") String motivo) { }
