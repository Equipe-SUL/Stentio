package com.example.stentio.core.dto;

import com.example.stentio.core.model.UnidadeCobranca;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Saída traz nomes de idiomas e tipos, para o front exibir "Inglês → Português" sem requisições extras.
public record RecursoResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        boolean ativo,
        List<TipoServicoResumo> tiposServico,
        List<PrecoResponse> precos
) {

    public record TipoServicoResumo(UUID id, String nome) {
    }

    public record IdiomaResumo(UUID id, String nome, String codigoIso) {
    }

    public record PrecoResponse(
            UUID id,
            IdiomaResumo idiomaOrigem,
            IdiomaResumo idiomaDestino,
            UnidadeCobranca unidade,
            BigDecimal valor
    ) {
    }
}
