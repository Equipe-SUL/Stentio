package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-015 CA.3: tudo o que o gestor precisa ver antes de decidir.
 *
 * <p>O caminho do arquivo no storage não aparece em lugar nenhum: o front recebe a rota de
 * download, e o caminho real viaja só no payload da fila, como no e-mail de orçamento.
 */
public record EntregaFinalResponse(
        @JsonProperty("ordem_servico_id") UUID ordemServicoId,
        String codigo,
        StatusOrdemServico status,
        @JsonProperty("motivo_revisao") String motivoRevisao,
        Cliente cliente,
        List<Etapa> etapas,
        Pacote pacote
) {
    public record Cliente(UUID id, String nome, String email) {
    }

    public record Etapa(
            @JsonProperty("etapa_id") UUID etapaId,
            String nome,
            int ordem,
            StatusEtapaOrdemServico status,
            @JsonProperty("entregue_em") Instant entregueEm,
            @JsonProperty("arquivo_nome") String arquivoNome,
            @JsonProperty("arquivo_url") String arquivoUrl
    ) {
    }

    /**
     * RN.3: a revisão mostra todas as etapas, mas o cliente recebe só o arquivo da última.
     *
     * <p>{@code bloqueios} existe para a tela poder explicar o botão desabilitado em vez de
     * só apagá-lo. Vazio quando {@code podeEnviar} é verdadeiro.
     */
    public record Pacote(
            @JsonProperty("etapa_id") UUID etapaId,
            @JsonProperty("arquivo_nome") String arquivoNome,
            @JsonProperty("tipo_conteudo") String tipoConteudo,
            @JsonProperty("tamanho_bytes") Long tamanhoBytes,
            @JsonProperty("pode_enviar") boolean podeEnviar,
            List<String> bloqueios
    ) {
    }
}
