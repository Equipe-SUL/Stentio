package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.model.StatusCertificacao;

import java.time.Instant;
import java.util.UUID;

/** US-024: a certificação como o portal e a fila do gestor a mostram. */
public record CertificacaoResponse(
        UUID id,
        @JsonProperty("recurso_id") UUID recursoId,
        @JsonProperty("recurso_nome") String recursoNome,
        @JsonProperty("idioma_origem") Idioma idiomaOrigem,
        @JsonProperty("idioma_destino") Idioma idiomaDestino,
        StatusCertificacao status,
        @JsonProperty("arquivo_nome") String arquivoNome,
        @JsonProperty("enviado_em") Instant enviadoEm,
        @JsonProperty("analisado_em") Instant analisadoEm,
        @JsonProperty("motivo_rejeicao") String motivoRejeicao
) {
    public record Idioma(UUID id, String nome, @JsonProperty("codigo_iso") String codigoIso) {
    }

    /**
     * O caminho do arquivo no storage nunca entra aqui: quem precisa do documento usa a
     * rota de download, que confere quem está pedindo.
     */
    public static CertificacaoResponse de(RecursoCertificacao certificacao) {
        return new CertificacaoResponse(
                certificacao.getId(),
                certificacao.getRecurso().getId(),
                certificacao.getRecurso().getNome(),
                paraIdioma(certificacao.getIdiomaOrigem()),
                paraIdioma(certificacao.getIdiomaDestino()),
                certificacao.getStatus(),
                certificacao.getArquivoNome(),
                certificacao.getEnviadoEm(),
                certificacao.getAnalisadoEm(),
                certificacao.getMotivoRejeicao());
    }

    private static Idioma paraIdioma(com.stentio.core.model.Idioma idioma) {
        return new Idioma(idioma.getId(), idioma.getNome(), idioma.getCodigoIso());
    }
}
