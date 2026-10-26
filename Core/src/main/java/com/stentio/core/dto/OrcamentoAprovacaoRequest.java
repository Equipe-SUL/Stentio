package com.stentio.core.dto;

import jakarta.validation.constraints.NotBlank;

public record OrcamentoAprovacaoRequest(@NotBlank String token) { }
