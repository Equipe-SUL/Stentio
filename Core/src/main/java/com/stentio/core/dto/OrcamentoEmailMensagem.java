package com.stentio.core.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * US-13: contrato do e-mail de aprovação entre o Core e o email-service.
 *
 * <p>O Core envia <b>dados</b>, não texto. O corpo do e-mail precisa dos dados da empresa
 * remetente (nome, CNPJ, endereço, telefone), que ficam no Mongo do email-service e não
 * existem aqui — então quem monta o corpo é quem tem todas as peças.
 *
 * <p>As datas viajam como texto ISO-8601 de propósito: o contrato não depende de nenhum
 * módulo de data do Jackson estar registrado nas duas pontas, e a formatação em pt-BR é
 * responsabilidade de quem renderiza.
 */
public record OrcamentoEmailMensagem(
        String to,
        String subject,
        Cliente cliente,
        Orcamento orcamento,
        // Nulo quando o orçamento foi criado sem vínculo com solicitação (a relação é opcional).
        Solicitacao solicitacao,
        List<Anexo> anexos) {

    public record Cliente(String nomeRepresentante, String nomeEmpresa) {
    }

    public record Orcamento(String codigo, int numeroVersao, BigDecimal valorTotal,
                            String linkAprovacao, String dataExpiracao) {
    }

    public record Solicitacao(String categoria, String idiomaOrigem, String idiomaDestino,
                              String descricao, String dataRecebimento) {
    }

    /**
     * O caminho é relativo ao diretório de uploads, igual ao que está no banco. O email-service
     * lê o mesmo volume em modo somente leitura; nenhum caminho absoluto do Core atravessa a fila.
     */
    public record Anexo(String nomeArquivo, String caminho, String tipoConteudo) {
    }
}
