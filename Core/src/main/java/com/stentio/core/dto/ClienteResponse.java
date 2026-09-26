package com.stentio.core.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nomeEmpresa,
        String nomeRepresentante,
        String emailRepresentante,
        String telefone,
        UUID empresaId,
        String cpfCnpj,
        LocalDateTime dataCadastro,
        LocalDateTime dataModificacao
) {
}
