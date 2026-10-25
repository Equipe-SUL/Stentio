package com.stentio.emailservice.dto;

import java.util.List;

/**
 * Mensagem já pronta para o SMTP: quem monta o conteúdo entrega isto, e o EmailService
 * só cuida do transporte. Mantém o EmailService sem conhecer regra de nenhum fluxo.
 *
 * @param html            corpo HTML; nulo envia só texto puro
 * @param texto           alternativa em texto puro, sempre obrigatória (clientes de e-mail
 *                        que não renderizam HTML, e também filtros de spam, usam esta parte)
 * @param imagemInline    imagem referenciada no HTML por {@code cid:}; nula quando não há
 */
public record MensagemEmail(
        String destinatario,
        String assunto,
        String texto,
        String html,
        List<Anexo> anexos,
        ImagemInline imagemInline) {

    public record Anexo(String nome, byte[] conteudo, String tipoConteudo) {
    }

    public record ImagemInline(String cid, byte[] conteudo, String tipoConteudo) {
    }

    public static MensagemEmail textoSimples(String destinatario, String assunto, String texto) {
        return new MensagemEmail(destinatario, assunto, texto, null, List.of(), null);
    }

    public List<Anexo> anexos() {
        return anexos == null ? List.of() : anexos;
    }
}
