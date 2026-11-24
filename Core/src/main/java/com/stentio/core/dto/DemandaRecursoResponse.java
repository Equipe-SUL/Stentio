package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-023 CA.5: uma demanda na tela do profissional.
 *
 * <p>É de propósito mais pobre que a visão da equipe. O profissional precisa saber o que
 * fazer, em que par de idiomas e até quando — e nada além disso. Cliente, valores e
 * demais etapas da mesma ordem não entram: são dados do negócio da Aliança, e quem executa
 * uma etapa não precisa deles para executá-la.
 *
 * <p>Os anexos originais do cliente ficam em {@code arquivos}; {@code arquivoEntrada} é a
 * entrega da etapa anterior, quando disponível. Ambos só são enviados para o profissional
 * responsável pela etapa, nunca para um convite aberto.
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
        @JsonProperty("data_limite_entrega") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataLimiteEntrega,
        @JsonProperty("entregue_em") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant entregueEm,
        @JsonProperty("arquivo_nome") String arquivoNome,
        @JsonProperty("arquivo_entrega_url") String arquivoEntregaUrl,
        @JsonProperty("arquivo_entrada") ArquivoEtapaAnterior arquivoEntrada,
        List<ArquivoDaSolicitacao> arquivos
) {

    /** Um anexo do cliente. A {@code url} é a rota autenticada do portal, nunca o disco. */
    public record ArquivoDaSolicitacao(
            UUID id,
            String nome,
            @JsonProperty("tamanho_bytes") long tamanhoBytes,
            String url
    ) {
    }

    /** A entrega da etapa anterior, que serve de entrada para esta etapa. */
    public record ArquivoEtapaAnterior(String nome, String url) {
    }
}
