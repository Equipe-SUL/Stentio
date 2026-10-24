package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * US-13: monta o corpo do e-mail de aprovação do orçamento.
 *
 * <p>Gera as duas partes da mensagem: o HTML, que é o que o cliente vê, e o texto puro,
 * que é a alternativa para quem bloqueia HTML. As duas levam o mesmo conteúdo.
 *
 * <p>Nada aqui consulta banco nem lê arquivo: recebe tudo pronto e devolve texto, o que
 * deixa a formatação testável sem subir Mongo nem SMTP.
 */
@Component
public class OrcamentoEmailRenderer {

    public static final String CID_LOGO = "logoEmpresa";

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRASIL);
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", BRASIL);
    private static final String SEM_INFORMACAO = "não informado";

    public CorpoEmail renderizar(OrcamentoEmailMensagem mensagem,
                                 ConfiguracaoEmpresaDocument empresa,
                                 AnexoSolicitacaoStorage.Resultado anexos,
                                 boolean logoInline) {
        return new CorpoEmail(
                texto(mensagem, empresa, anexos),
                html(mensagem, empresa, anexos, logoInline));
    }

    // ---------------------------------------------------------------- texto puro

    private String texto(OrcamentoEmailMensagem mensagem,
                         ConfiguracaoEmpresaDocument empresa,
                         AnexoSolicitacaoStorage.Resultado anexos) {
        OrcamentoEmailMensagem.Orcamento orcamento = mensagem.orcamento();
        StringBuilder corpo = new StringBuilder();

        corpo.append(saudacao(mensagem)).append(",\n\n")
                .append("O orçamento ").append(orcamento.codigo())
                .append(" está pronto para sua aprovação.\n\n")
                .append("Valor total: ").append(moeda(orcamento.valorTotal())).append('\n')
                .append("Validade: ").append(dataHora(orcamento.dataExpiracao())).append('\n')
                .append("Aprovar ou recusar: ").append(orcamento.linkAprovacao()).append("\n\n");

        OrcamentoEmailMensagem.Solicitacao solicitacao = mensagem.solicitacao();
        if (solicitacao != null) {
            corpo.append("DADOS DA SOLICITAÇÃO\n")
                    .append("Categoria do projeto: ").append(ou(solicitacao.categoria())).append('\n')
                    .append("Idioma de origem: ").append(ou(solicitacao.idiomaOrigem())).append('\n')
                    .append("Idioma de destino: ").append(ou(solicitacao.idiomaDestino())).append('\n')
                    .append("Data de recebimento: ").append(data(solicitacao.dataRecebimento())).append('\n')
                    .append("Descrição: ").append(ou(solicitacao.descricao())).append("\n\n");
        }

        if (!anexos.carregados().isEmpty()) {
            corpo.append("ARQUIVOS EM ANEXO\n");
            anexos.carregados().forEach(anexo -> corpo.append("- ").append(anexo.nome()).append('\n'));
            corpo.append('\n');
        }
        if (!anexos.ignorados().isEmpty()) {
            corpo.append(avisoDeIgnorados(anexos.ignorados())).append("\n\n");
        }

        linhasDaEmpresa(empresa).forEach(linha -> corpo.append(linha).append('\n'));
        return corpo.toString();
    }

    // ---------------------------------------------------------------- HTML

    private String html(OrcamentoEmailMensagem mensagem,
                        ConfiguracaoEmpresaDocument empresa,
                        AnexoSolicitacaoStorage.Resultado anexos,
                        boolean logoInline) {
        OrcamentoEmailMensagem.Orcamento orcamento = mensagem.orcamento();
        StringBuilder corpo = new StringBuilder();

        corpo.append("""
                <!DOCTYPE html>
                <html lang="pt-BR"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="margin:0;padding:24px 12px;background:#f4f5f7;\
                font-family:Arial,Helvetica,sans-serif;color:#1f2933;">
                <div style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;\
                padding:32px;">
                """);

        if (logoInline) {
            corpo.append("<img src=\"cid:").append(CID_LOGO)
                    .append("\" alt=\"").append(escapar(nomeDaEmpresa(empresa)))
                    .append("\" style=\"max-height:56px;margin-bottom:24px;\">");
        }

        corpo.append("<p style=\"font-size:16px;margin:0 0 16px;\">")
                .append(escapar(saudacao(mensagem))).append(",</p>")
                .append("<p style=\"font-size:15px;line-height:1.5;margin:0 0 24px;\">O orçamento <strong>")
                .append(escapar(orcamento.codigo()))
                .append("</strong> está pronto para sua aprovação.</p>");

        // Valor e validade em destaque: é a informação que decide a leitura do e-mail.
        corpo.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"width:100%;background:#f4f5f7;border-radius:6px;margin:0 0 24px;\">")
                .append("<tr><td style=\"padding:16px;font-size:15px;\">")
                .append("<div style=\"font-size:22px;font-weight:bold;\">")
                .append(escapar(moeda(orcamento.valorTotal()))).append("</div>")
                .append("<div style=\"color:#52606d;margin-top:4px;\">Válido até ")
                .append(escapar(dataHora(orcamento.dataExpiracao()))).append("</div>")
                .append("</td></tr></table>");

        corpo.append("<p style=\"margin:0 0 28px;\">")
                .append("<a href=\"").append(escapar(orcamento.linkAprovacao()))
                .append("\" style=\"display:inline-block;background:#1f6feb;color:#ffffff;")
                .append("text-decoration:none;padding:12px 24px;border-radius:6px;font-size:15px;\">")
                .append("Aprovar ou recusar o orçamento</a></p>");

        OrcamentoEmailMensagem.Solicitacao solicitacao = mensagem.solicitacao();
        if (solicitacao != null) {
            corpo.append(titulo("Dados da solicitação"))
                    .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" ")
                    .append("style=\"width:100%;font-size:14px;line-height:1.6;margin:0 0 24px;\">")
                    .append(linha("Categoria do projeto", solicitacao.categoria()))
                    .append(linha("Idioma de origem", solicitacao.idiomaOrigem()))
                    .append(linha("Idioma de destino", solicitacao.idiomaDestino()))
                    .append(linha("Data de recebimento", data(solicitacao.dataRecebimento())))
                    .append(linha("Descrição", solicitacao.descricao()))
                    .append("</table>");
        }

        if (!anexos.carregados().isEmpty()) {
            corpo.append(titulo("Arquivos em anexo"))
                    .append("<ul style=\"font-size:14px;line-height:1.6;margin:0 0 24px;padding-left:20px;\">");
            anexos.carregados().forEach(anexo -> corpo.append("<li>")
                    .append(escapar(anexo.nome())).append("</li>"));
            corpo.append("</ul>");
        }
        if (!anexos.ignorados().isEmpty()) {
            corpo.append("<p style=\"font-size:13px;color:#8a6d00;background:#fff8e1;")
                    .append("padding:12px;border-radius:6px;margin:0 0 24px;\">")
                    .append(escapar(avisoDeIgnorados(anexos.ignorados()))).append("</p>");
        }

        corpo.append("<hr style=\"border:none;border-top:1px solid #e4e7eb;margin:0 0 16px;\">")
                .append("<div style=\"font-size:13px;line-height:1.6;color:#52606d;\">");
        linhasDaEmpresa(empresa).forEach(linha -> corpo.append(escapar(linha)).append("<br>"));
        corpo.append("</div></div></body></html>");
        return corpo.toString();
    }

    private static String titulo(String texto) {
        return "<h2 style=\"font-size:15px;text-transform:uppercase;letter-spacing:.5px;"
                + "color:#52606d;margin:0 0 12px;\">" + escapar(texto) + "</h2>";
    }

    private static String linha(String rotulo, String valor) {
        return "<tr><td style=\"padding:4px 12px 4px 0;color:#52606d;white-space:nowrap;"
                + "vertical-align:top;\">" + escapar(rotulo) + "</td>"
                + "<td style=\"padding:4px 0;\">" + escapar(ou(valor)) + "</td></tr>";
    }

    // ---------------------------------------------------------------- dados compartilhados

    private static String saudacao(OrcamentoEmailMensagem mensagem) {
        OrcamentoEmailMensagem.Cliente cliente = mensagem.cliente();
        if (cliente == null) {
            return "Olá";
        }
        // Prefere o nome da pessoa; sem ele, o da empresa ainda personaliza mais que "Olá".
        String nome = preenchido(cliente.nomeRepresentante())
                ? cliente.nomeRepresentante()
                : cliente.nomeEmpresa();
        return preenchido(nome) ? "Olá, " + nome.strip() : "Olá";
    }

    /**
     * Dados da empresa que usa o sistema, para o cliente entrar em contato. Ficam no Mongo
     * do email-service e podem não estar cadastrados ainda — nesse caso o e-mail sai sem
     * o rodapé, em vez de não sair.
     */
    private static List<String> linhasDaEmpresa(ConfiguracaoEmpresaDocument empresa) {
        if (empresa == null) {
            return List.of();
        }
        List<String> linhas = new ArrayList<>();
        if (preenchido(empresa.getNome())) {
            linhas.add(empresa.getNome().strip());
        }
        if (preenchido(empresa.getCnpj())) {
            linhas.add("CNPJ: " + empresa.getCnpj().strip());
        }
        if (preenchido(empresa.getEndereco())) {
            linhas.add(empresa.getEndereco().strip());
        }
        if (preenchido(empresa.getTelefone())) {
            linhas.add("Telefone: " + empresa.getTelefone().strip());
        }
        if (!linhas.isEmpty()) {
            linhas.add("Em caso de dúvida, responda este e-mail ou fale com a gente pelos contatos acima.");
        }
        return linhas;
    }

    private static String nomeDaEmpresa(ConfiguracaoEmpresaDocument empresa) {
        return empresa != null && preenchido(empresa.getNome()) ? empresa.getNome().strip() : "Stentio";
    }

    private static String avisoDeIgnorados(List<String> ignorados) {
        return "Não foi possível anexar: " + String.join(", ", ignorados)
                + ". Você pode solicitar esses arquivos respondendo este e-mail.";
    }

    // ---------------------------------------------------------------- formatação

    private static String moeda(BigDecimal valor) {
        if (valor == null) {
            return SEM_INFORMACAO;
        }
        // Espaço estreito do formatador padrão (R$ 1.500,00) viraria caractere estranho em
        // alguns clientes de e-mail, então a formatação do número é separada do símbolo.
        return "R$ " + String.format(BRASIL, "%,.2f", valor);
    }

    private static String data(String iso) {
        return formatar(iso, DATA);
    }

    private static String dataHora(String iso) {
        return formatar(iso, DATA_HORA);
    }

    /**
     * A data chega como texto ISO-8601 — {@code Instant} quando vem do orçamento e
     * {@code LocalDateTime} quando vem da solicitação. Formato inesperado volta como veio:
     * uma data estranha no corpo é melhor que um e-mail que não sai.
     */
    private static String formatar(String iso, DateTimeFormatter formato) {
        if (!preenchido(iso)) {
            return SEM_INFORMACAO;
        }
        try {
            return formato.format(Instant.parse(iso).atZone(FUSO));
        } catch (DateTimeParseException ignorado) {
            // Não é instante com fuso; tenta data e hora local e, por último, só data.
        }
        try {
            return formato.format(LocalDateTime.parse(iso).atZone(FUSO));
        } catch (DateTimeParseException ignorado) {
            // Segue para a última tentativa.
        }
        try {
            return formato.format(LocalDate.parse(iso).atStartOfDay(FUSO));
        } catch (DateTimeParseException ignorado) {
            return iso;
        }
    }

    private static String ou(String valor) {
        return preenchido(valor) ? valor.strip() : SEM_INFORMACAO;
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    // O nome do cliente, a descrição e os nomes de arquivo vêm de quem preencheu a
    // solicitação, então nunca entram crus no HTML.
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
