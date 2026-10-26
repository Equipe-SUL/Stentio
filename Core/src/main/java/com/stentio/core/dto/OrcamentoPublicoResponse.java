package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrcamentoPublicoResponse(
        UUID id,
        StatusOrcamento status,
        @JsonProperty("valor_total") BigDecimal valorTotal,
        @JsonProperty("data_expiracao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataExpiracao,
        List<Item> itens,
        @JsonProperty("numero_versao") Integer numeroVersao,
        @JsonProperty("criado_em") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant criadoEm,
        SolicitacaoResumo solicitacao) {
    public static OrcamentoPublicoResponse de(Orcamento orcamento) {
        // US-11: os itens passaram a ser pares de idiomas com serviços. O link público mantém a
        // lista plana de serviços, na ordem dos itens, para não quebrar quem já consome este formato.
        List<Item> servicos = orcamento.getItens().stream()
                .flatMap(item -> item.getServicos().stream())
                .map(Item::de)
                .toList();

        SolicitacaoResumo solicitacaoResumo = null;
        if (orcamento.getSolicitacao() != null) {
            solicitacaoResumo = SolicitacaoResumo.de(orcamento.getSolicitacao());
        }

        return new OrcamentoPublicoResponse(
                orcamento.getId(),
                orcamento.getStatus(),
                orcamento.getValorTotal(),
                orcamento.getDataExpiracao(),
                servicos,
                orcamento.getNumeroVersao(),
                orcamento.getCriadoEm(),
                solicitacaoResumo);
    }

    public record Item(
            @JsonProperty("tipo_de_serviço") String tipoServico,
            BigDecimal quantidade,
            String unidade,
            @JsonProperty("valor_unitario") BigDecimal valorUnitario,
            BigDecimal subtotal) {
        static Item de(OrcamentoItemServico servico) {
            String unidade = servico.getUnidade() == null ? null : servico.getUnidade().name();
            return new Item(servico.getTipoServico(), servico.getQuantidade(), unidade,
                    servico.getValorUnitario(), servico.getSubtotal());
        }
    }

    public record SolicitacaoResumo(
            UUID id,
            String descricao,
            @JsonProperty("data_recebimento") @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime dataRecebimento,
            CategoriaResumo categoria,
            @JsonProperty("idioma_origem") IdiomaResumo idiomaOrigem,
            @JsonProperty("idioma_destino") IdiomaResumo idiomaDestino,
            List<ArquivoResumoPublico> arquivos) {
        static SolicitacaoResumo de(Solicitacao solicitacao) {
            List<ArquivoResumoPublico> arquivos = solicitacao.getArquivos() == null
                    ? List.of()
                    : solicitacao.getArquivos().stream()
                    .map(ArquivoResumoPublico::de)
                    .toList();

            return new SolicitacaoResumo(
                    solicitacao.getId(),
                    solicitacao.getDescricao(),
                    solicitacao.getDataRecebimento(),
                    solicitacao.getCategoriaProjeto() == null ? null : CategoriaResumo.de(solicitacao.getCategoriaProjeto()),
                    solicitacao.getIdiomaOrigem() == null ? null : IdiomaResumo.de(solicitacao.getIdiomaOrigem()),
                    solicitacao.getIdiomaDestino() == null ? null : IdiomaResumo.de(solicitacao.getIdiomaDestino()),
                    arquivos);
        }
    }

    public record CategoriaResumo(UUID id, String nome) {
        static CategoriaResumo de(com.stentio.core.model.CategoriaProjeto categoriaProjeto) {
            return new CategoriaResumo(categoriaProjeto.getId(), categoriaProjeto.getNome());
        }
    }

    public record IdiomaResumo(UUID id, String nome,
                               @JsonProperty("codigo_iso") String codigoIso) {
        static IdiomaResumo de(com.stentio.core.model.Idioma idioma) {
            return new IdiomaResumo(idioma.getId(), idioma.getNome(), idioma.getCodigoIso());
        }
    }

    public record ArquivoResumoPublico(
            UUID id,
            String nome,
            @JsonProperty("tipo_conteudo") String tipoConteudo,
            @JsonProperty("tamanho_bytes") long tamanhoBytes) {
        static ArquivoResumoPublico de(ArquivoSolicitacao arquivo) {
            return new ArquivoResumoPublico(
                    arquivo.getId(),
                    arquivo.getNomeOriginal(),
                    arquivo.getTipoConteudo(),
                    arquivo.getTamanhoBytes());
        }
    }
}
