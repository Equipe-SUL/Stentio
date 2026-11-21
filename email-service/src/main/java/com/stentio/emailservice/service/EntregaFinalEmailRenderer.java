package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.EntregaFinalEmailMensagem;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import org.springframework.stereotype.Component;

/**
 * US-015: o corpo do e-mail que leva o trabalho pronto ao cliente.
 *
 * <p>Texto e HTML lado a lado, como o e-mail do orçamento: cliente de e-mail que não
 * renderiza HTML ainda recebe a mensagem legível.
 *
 * <p>O texto não afirma que o arquivo foi recebido nem pede confirmação de leitura — o
 * sistema sabe que publicou a mensagem, e nada além disso (RN.7).
 */
@Component
public class EntregaFinalEmailRenderer {

    public CorpoEmail renderizar(EntregaFinalEmailMensagem mensagem, ConfiguracaoEmpresaDocument empresa) {
        String saudacao = nomeDoRepresentante(mensagem);
        String codigo = mensagem.ordemServico() == null ? "" : mensagem.ordemServico().codigo();
        String nomeArquivo = mensagem.anexo() == null ? "" : mensagem.anexo().nomeArquivo();
        String recado = mensagem.mensagem();

        StringBuilder texto = new StringBuilder()
                .append("Olá, ").append(saudacao).append(".\n\n")
                .append("O serviço da ordem ").append(codigo).append(" foi concluído.\n")
                .append("O documento final segue em anexo: ").append(nomeArquivo).append("\n");

        if (recado != null && !recado.isBlank()) {
            texto.append('\n').append(recado).append('\n');
        }

        texto.append("\nQualquer ajuste necessário, é só responder a este e-mail.\n\n")
                .append(assinatura(empresa)).append('\n');

        return new CorpoEmail(texto.toString(), html(saudacao, codigo, nomeArquivo, recado, empresa));
    }

    private static String nomeDoRepresentante(EntregaFinalEmailMensagem mensagem) {
        if (mensagem.cliente() == null) {
            return "tudo bem";
        }
        String nome = mensagem.cliente().nomeRepresentante();
        return nome == null || nome.isBlank() ? "tudo bem" : nome;
    }

    /** O cadastro da empresa é opcional, então a assinatura cai num padrão em vez de sumir. */
    private static String assinatura(ConfiguracaoEmpresaDocument empresa) {
        if (empresa == null || empresa.getNome() == null || empresa.getNome().isBlank()) {
            return "Equipe Aliança Traduções";
        }
        return "Equipe " + empresa.getNome();
    }

    private static String html(String saudacao, String codigo, String nomeArquivo,
                               String recado, ConfiguracaoEmpresaDocument empresa) {
        StringBuilder html = new StringBuilder()
                .append("<p>Olá, ").append(escapar(saudacao)).append(".</p>")
                .append("<p>O serviço da ordem <strong>").append(escapar(codigo))
                .append("</strong> foi concluído.<br>O documento final segue em anexo: ")
                .append(escapar(nomeArquivo)).append("</p>");

        if (recado != null && !recado.isBlank()) {
            html.append("<p>").append(escapar(recado)).append("</p>");
        }

        return html.append("<p>Qualquer ajuste necessário, é só responder a este e-mail.</p>")
                .append("<p>").append(escapar(assinatura(empresa))).append("</p>")
                .toString();
    }

    /**
     * O nome do representante e o nome do arquivo vêm de cadastro e de upload: texto de
     * terceiro dentro de HTML que vai para a caixa de outra pessoa.
     */
    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record CorpoEmail(String texto, String html) {
    }
}
