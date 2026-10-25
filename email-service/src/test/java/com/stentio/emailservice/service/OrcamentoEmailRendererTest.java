package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// US-13: o corpo do e-mail é texto puro, então dá para conferir cada exigência do card
// sem subir Mongo, SMTP nem RabbitMQ.
class OrcamentoEmailRendererTest {

    private final OrcamentoEmailRenderer renderer = new OrcamentoEmailRenderer();

    private static final AnexoSolicitacaoStorage.Resultado SEM_ANEXOS =
            new AnexoSolicitacaoStorage.Resultado(List.of(), List.of());

    private static OrcamentoEmailMensagem mensagem(OrcamentoEmailMensagem.Cliente cliente,
                                                   OrcamentoEmailMensagem.Solicitacao solicitacao) {
        return new OrcamentoEmailMensagem(
                "cliente@teste.com",
                "Orçamento ORC-2026-ABCDEF aguardando sua aprovação",
                cliente,
                new OrcamentoEmailMensagem.Orcamento("ORC-2026-ABCDEF", 1,
                        new BigDecimal("1500.00"),
                        "https://stentio.local/api/v1/public/orcamentos?token=abc",
                        "2026-10-28T20:30:00Z"),
                solicitacao,
                List.of());
    }

    private static OrcamentoEmailMensagem.Solicitacao solicitacaoCompleta() {
        return new OrcamentoEmailMensagem.Solicitacao("Jurídico", "Português", "Inglês",
                "Contrato social & estatuto", "2026-10-20T09:15:00");
    }

    private static ConfiguracaoEmpresaDocument empresa() {
        return new ConfiguracaoEmpresaDocument(ConfiguracaoEmpresaDocument.ID_UNICO,
                "Aliança Traduções", "12.345.678/0001-99", "Rua das Flores, 100 - São Paulo/SP",
                "(11) 99999-0000");
    }

    @Test
    void saudacaoUsaONomeDoRepresentante() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria Souza", "Empresa XPTO"), null),
                null, SEM_ANEXOS, false);

        assertTrue(corpo.texto().startsWith("Olá, Maria Souza,"));
        assertTrue(corpo.html().contains("Olá, Maria Souza,"));
    }

    @Test
    void semRepresentanteCaiNoNomeDaEmpresaEDepoisEmOlaSozinho() {
        var comEmpresa = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente(null, "Empresa XPTO"), null),
                null, SEM_ANEXOS, false);
        assertTrue(comEmpresa.texto().startsWith("Olá, Empresa XPTO,"));

        var semNada = renderer.renderizar(mensagem(null, null), null, SEM_ANEXOS, false);
        assertTrue(semNada.texto().startsWith("Olá,"));
    }

    @Test
    void valorEDatasSaemNoFormatoBrasileiro() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacaoCompleta()),
                null, SEM_ANEXOS, false);

        assertTrue(corpo.texto().contains("R$ 1.500,00"), corpo.texto());
        // 20:30 UTC é 17:30 em São Paulo: o cliente lê a validade no fuso dele.
        assertTrue(corpo.texto().contains("28/10/2026 às 17:30"), corpo.texto());
        assertTrue(corpo.texto().contains("Data de recebimento: 20/10/2026"), corpo.texto());
    }

    @Test
    void corpoTrazOsDadosDaSolicitacaoEOLinkDeAprovacao() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacaoCompleta()),
                null, SEM_ANEXOS, false);

        assertTrue(corpo.texto().contains("Categoria do projeto: Jurídico"));
        assertTrue(corpo.texto().contains("Idioma de origem: Português"));
        assertTrue(corpo.texto().contains("Idioma de destino: Inglês"));
        assertTrue(corpo.texto().contains("Descrição: Contrato social & estatuto"));
        assertTrue(corpo.texto().contains("https://stentio.local/api/v1/public/orcamentos?token=abc"));
        assertTrue(corpo.html().contains("https://stentio.local/api/v1/public/orcamentos?token=abc"));
    }

    @Test
    void orcamentoSemSolicitacaoVinculadaNaoQuebraEOmiteASecao() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), null),
                empresa(), SEM_ANEXOS, false);

        assertFalse(corpo.texto().contains("DADOS DA SOLICITAÇÃO"));
        assertTrue(corpo.texto().contains("ORC-2026-ABCDEF"));
    }

    @Test
    void dadosDaEmpresaEntramNoRodapeParaOClienteEntrarEmContato() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacaoCompleta()),
                empresa(), SEM_ANEXOS, false);

        assertTrue(corpo.texto().contains("Aliança Traduções"));
        // O cadastro guarda o CNPJ sem pontuação; quem lê o e-mail é o cliente, então
        // ele volta com a máscara.
        assertTrue(corpo.texto().contains("CNPJ: 12.345.678/0001-99"), corpo.texto());
        assertTrue(corpo.texto().contains("Rua das Flores, 100 - São Paulo/SP"));
        assertTrue(corpo.texto().contains("Telefone: (11) 99999-0000"));
        assertTrue(corpo.html().contains("Aliança Tradu"));
    }

    @Test
    void cnpjForaDoTamanhoEsperadoSaiComoEstaGuardado() {
        var curto = new ConfiguracaoEmpresaDocument(ConfiguracaoEmpresaDocument.ID_UNICO,
                "Aliança Traduções", "123", "Rua 1", null);

        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), null),
                curto, SEM_ANEXOS, false);

        assertTrue(corpo.texto().contains("CNPJ: 123"));
    }

    @Test
    void empresaNaoCadastradaDeixaOEmailSairSemORodape() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacaoCompleta()),
                null, SEM_ANEXOS, false);

        assertFalse(corpo.texto().contains("CNPJ"));
        assertTrue(corpo.texto().contains("R$ 1.500,00"));
    }

    @Test
    void anexosAparecemNoCorpoEOsIgnoradosViramAviso() {
        var anexos = new AnexoSolicitacaoStorage.Resultado(
                List.of(new MensagemEmail.Anexo("contrato.pdf",
                        "x".getBytes(StandardCharsets.UTF_8), "application/pdf")),
                List.of("procuracao.docx"));

        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacaoCompleta()),
                empresa(), anexos, false);

        assertTrue(corpo.texto().contains("- contrato.pdf"));
        assertTrue(corpo.texto().contains("Não foi possível anexar: procuracao.docx"));
        assertTrue(corpo.html().contains("<li>contrato.pdf</li>"));
    }

    @Test
    void conteudoDoClienteNaoEntraCruNoHtml() {
        var solicitacao = new OrcamentoEmailMensagem.Solicitacao("Jurídico", "Português", "Inglês",
                "<script>alert('x')</script>", "2026-10-20T09:15:00");

        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("<b>Maria</b>", null), solicitacao),
                null, SEM_ANEXOS, false);

        assertFalse(corpo.html().contains("<script>"));
        assertTrue(corpo.html().contains("&lt;script&gt;"));
        assertTrue(corpo.html().contains("&lt;b&gt;Maria&lt;/b&gt;"));
    }

    @Test
    void logoDaEmpresaEhReferenciadaPorCidQuandoExiste() {
        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), null),
                empresa(), SEM_ANEXOS, true);

        assertTrue(corpo.html().contains("cid:" + OrcamentoEmailRenderer.CID_LOGO));
    }

    @Test
    void campoSemInformacaoNaoDeixaNullNoCorpo() {
        var solicitacao = new OrcamentoEmailMensagem.Solicitacao("Jurídico", "Português", "Inglês",
                null, null);

        var corpo = renderer.renderizar(
                mensagem(new OrcamentoEmailMensagem.Cliente("Maria", null), solicitacao),
                null, SEM_ANEXOS, false);

        assertFalse(corpo.texto().contains("null"));
        assertTrue(corpo.texto().contains("Descrição: não informado"));
    }
}
