package com.stentio.core.dto;

import java.util.UUID;

// Filtros opcionais da listagem (CA.5), recebidos como query params: ?tipoServicoId=...&idiomaOrigemId=...
public record RecursoFiltro(
        UUID tipoServicoId,
        UUID idiomaOrigemId,
        UUID idiomaDestinoId,
        Boolean ativo
) {
}
