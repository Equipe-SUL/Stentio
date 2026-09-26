package com.stentio.core.dto;

import java.util.UUID;

public record ClienteFiltro(
        String termo,
        UUID empresaId,
        String cpfCnpj,
        String emailRepresentante
) {
}
