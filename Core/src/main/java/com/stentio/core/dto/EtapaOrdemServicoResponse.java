package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.time.Instant;
import java.util.UUID;

/**
 * US-011: uma etapa na resposta. O tipo de serviço vai como id mais nome, no mesmo formato
 * que a listagem de OS usa para o cliente — a tela mostra o nome e guarda o id para editar.
 *
 * <p>US-013: {@code status} é o estado gravado da etapa. Na US-012 ele era calculado, para
 * não perder quem podia começar ao marcar uma etapa como alocada; agora que o estado da
 * etapa anterior responde isso, gravar voltou a ser suficiente. Os valores que o front lê
 * continuam os mesmos, com os novos somados.
 */
public record EtapaOrdemServicoResponse(
        UUID id,
        Integer ordem,
        String nome,
        @JsonProperty("tipo_servico_id") UUID tipoServicoId,
        @JsonProperty("tipo_servico_nome") String tipoServicoNome,
        StatusEtapaOrdemServico status,
        @JsonProperty("prazo_estimado_horas") Integer prazoEstimadoHoras,
        @JsonProperty("recurso_id") UUID recursoId,
        @JsonProperty("recurso_nome") String recursoNome,
        @JsonProperty("data_limite_entrega") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimiteEntrega,
        @JsonProperty("aceito_em") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant aceitoEm,
        @JsonProperty("entregue_em") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant entregueEm,
        @JsonProperty("arquivo_nome") String arquivoNome
) {
}
