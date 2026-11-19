package com.stentio.core.dto;

import jakarta.validation.constraints.NotNull;

/**
 * US-023 CA.6: corpo do PATCH /api/v1/recursos/{id}/verificacao — { "verificado": true }.
 *
 * <p>Separado do {@code AlteracaoStatusRequest} de propósito: ativar e verificar são
 * decisões diferentes. Um recurso pode estar ativo e ainda não verificado, e juntar os
 * dois em um corpo só convidaria a mexer em um querendo mexer no outro.
 *
 * <p>Aceita falso também, e não só a confirmação: o gestor que verificou por engano
 * precisa conseguir desfazer sem pedir ajuda ao banco.
 */
public record VerificacaoRecursoRequest(

        @NotNull(message = "Informe se o recurso está verificado")
        Boolean verificado
) {
}
