package com.stentio.core.dto;

import java.util.UUID;

// Filtros opcionais da listagem (CA.5), recebidos como query params: ?tipoServicoId=...&idiomaOrigemId=...
public record RecursoFiltro(
        UUID tipoServicoId,
        UUID idiomaOrigemId,
        UUID idiomaDestinoId,
        Boolean ativo,
        // US-023 CA.6: ?verificado=false é como o gestor acha a fila de quem está esperando.
        Boolean verificado
) {
}
