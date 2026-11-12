package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.UnidadeCobranca;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * US-012 CA.2: um profissional apto a executar a etapa.
 *
 * <p>Vai mais enxuto que o {@code RecursoResponse} do cadastro: aqui a tela escolhe
 * alguém, não edita o recurso. Os preços vêm filtrados pelo par consultado — é o que
 * permite ao gestor escolher por custo sem abrir o cadastro de cada um.
 */
public record RecursoDisponivelResponse(
        UUID id,
        String nome,
        String email,
        @JsonProperty("precos_do_par") List<PrecoDoPar> precosDoPar
) {

    public record PrecoDoPar(UnidadeCobranca unidade, BigDecimal valor) {
    }
}
