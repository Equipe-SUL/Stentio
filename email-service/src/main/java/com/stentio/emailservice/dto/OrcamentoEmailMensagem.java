package com.stentio.emailservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * US-13: dados que o Core publica em {@code email.orcamento.queue}. Espelha
 * {@code com.stentio.core.dto.OrcamentoEmailMensagem} — quem alterar um lado altera o outro.
 *
 * <p>{@code ignoreUnknown} em todos os níveis: se o Core passar a enviar um campo novo,
 * o e-mail continua saindo em vez de a mensagem morrer na fila.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrcamentoEmailMensagem(
        String to,
        String subject,
        Cliente cliente,
        Orcamento orcamento,
        Solicitacao solicitacao,
        List<Anexo> anexos) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cliente(String nomeRepresentante, String nomeEmpresa) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Orcamento(String codigo, int numeroVersao, BigDecimal valorTotal,
                            String linkAprovacao, String dataExpiracao) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Solicitacao(String categoria, String idiomaOrigem, String idiomaDestino,
                              String descricao, String dataRecebimento) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Anexo(String nomeArquivo, String caminho, String tipoConteudo) {
    }

    public List<Anexo> anexos() {
        return anexos == null ? List.of() : anexos;
    }
}
