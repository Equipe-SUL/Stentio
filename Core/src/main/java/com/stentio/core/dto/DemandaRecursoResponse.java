package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.time.Instant;
import java.util.UUID;

/**
 * US-023 CA.5: uma demanda na tela do profissional.
 *
 * <p>É de propósito mais pobre que a visão da equipe. O profissional precisa saber o que
 * fazer, em que par de idiomas e até quando — e nada além disso. Cliente, valores e
 * demais etapas da mesma ordem não entram: são dados do negócio da Aliança, e quem executa
 * uma etapa não precisa deles para executá-la.
 */
public record DemandaRecursoResponse(
        @JsonProperty("etapa_id") UUID etapaId,
        @JsonProperty("ordem_servico_id") UUID ordemServicoId,
        @JsonProperty("ordem_servico_codigo") String ordemServicoCodigo,
        Integer ordem,
        String nome,
        @JsonProperty("tipo_servico") String tipoServico,
        @JsonProperty("idioma_origem") String idiomaOrigem,
        @JsonProperty("idioma_destino") String idiomaDestino,
        StatusEtapaOrdemServico status,
        boolean liberada,
        @JsonProperty("prazo_estimado_horas") Integer prazoEstimadoHoras,
        @JsonProperty("data_limite_entrega") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimiteEntrega
) {
}
