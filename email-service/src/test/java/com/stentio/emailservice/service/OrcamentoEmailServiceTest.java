package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;
import com.stentio.emailservice.repository.ConfiguracaoEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// US-13: junção das três fontes — fila, Mongo e volume de anexos.
class OrcamentoEmailServiceTest {

    @TempDir
    Path uploads;

    private final ConfiguracaoEmpresaRepository empresas = mock(ConfiguracaoEmpresaRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private OrcamentoEmailService service;

    @BeforeEach
    void preparar() throws IOException {
        Files.createDirectories(uploads.resolve("2026/10"));
        Files.writeString(uploads.resolve("2026/10/a.pdf"), "contrato");
        service = new OrcamentoEmailService(empresas,
                new AnexoSolicitacaoStorage(uploads.toString(), 10_485_760L),
                new OrcamentoEmailRenderer(), emailService);
    }

    private static OrcamentoEmailMensagem mensagem(List<OrcamentoEmailMensagem.Anexo> anexos) {
        return new OrcamentoEmailMensagem(
                "cliente@teste.com",
                "Orçamento ORC-2026-ABCDEF aguardando sua aprovação",
                new OrcamentoEmailMensagem.Cliente("Maria Souza", "Empresa XPTO"),
                new OrcamentoEmailMensagem.Orcamento("ORC-2026-ABCDEF", 1, new BigDecimal("1500.00"),
                        "https://stentio.local/publico?token=abc", "2026-10-28T20:30:00Z"),
                new OrcamentoEmailMensagem.Solicitacao("Jurídico", "Português", "Inglês",
                        "Contrato social", "2026-10-20T09:15:00"),
                anexos);
    }

    private MensagemEmail capturarEnvio() {
        var captor = org.mockito.ArgumentCaptor.forClass(MensagemEmail.class);
        verify(emailService).enviar(captor.capture());
        return captor.getValue();
    }

    @Test
    void montaOEmailComDadosDaEmpresaEOsAnexosDaSolicitacao() {
        when(empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO))
                .thenReturn(Optional.of(new ConfiguracaoEmpresaDocument(
                        ConfiguracaoEmpresaDocument.ID_UNICO, "Aliança Traduções",
                        "12.345.678/0001-99", "Rua das Flores, 100", "(11) 99999-0000")));

        service.enviar(mensagem(List.of(
                new OrcamentoEmailMensagem.Anexo("contrato.pdf", "2026/10/a.pdf", "application/pdf"))));

        MensagemEmail enviada = capturarEnvio();
        assertEquals("cliente@teste.com", enviada.destinatario());
        assertEquals("Orçamento ORC-2026-ABCDEF aguardando sua aprovação", enviada.assunto());
        assertTrue(enviada.texto().contains("Olá, Maria Souza"));
        assertTrue(enviada.texto().contains("Aliança Traduções"));
        assertNotNull(enviada.html());
        assertEquals(1, enviada.anexos().size());
        assertEquals("contrato.pdf", enviada.anexos().get(0).nome());
        // Empresa sem logo cadastrada não gera imagem embutida.
        assertNull(enviada.imagemInline());
    }

    @Test
    void empresaNaoCadastradaNaoImpedeOEnvio() {
        when(empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO)).thenReturn(Optional.empty());

        service.enviar(mensagem(List.of()));

        MensagemEmail enviada = capturarEnvio();
        assertFalse(enviada.texto().contains("CNPJ"));
        assertTrue(enviada.texto().contains("ORC-2026-ABCDEF"));
    }

    @Test
    void logoCadastradaViraImagemEmbutidaNoHtml() {
        ConfiguracaoEmpresaDocument empresa = new ConfiguracaoEmpresaDocument(
                ConfiguracaoEmpresaDocument.ID_UNICO, "Stentio", "12345678000199", "Rua 1", null);
        empresa.atualizarLogo(new byte[]{1, 2, 3}, "image/png", "logo.png");
        when(empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO)).thenReturn(Optional.of(empresa));

        service.enviar(mensagem(List.of()));

        MensagemEmail enviada = capturarEnvio();
        assertNotNull(enviada.imagemInline());
        assertEquals(OrcamentoEmailRenderer.CID_LOGO, enviada.imagemInline().cid());
        assertTrue(enviada.html().contains("cid:" + OrcamentoEmailRenderer.CID_LOGO));
    }

    @Test
    void anexoQueSumiuDoVolumeViraAvisoNoCorpoEOEmailSai() {
        when(empresas.findById(ConfiguracaoEmpresaDocument.ID_UNICO)).thenReturn(Optional.empty());

        service.enviar(mensagem(List.of(
                new OrcamentoEmailMensagem.Anexo("sumido.pdf", "2026/10/nao-existe.pdf", "application/pdf"))));

        MensagemEmail enviada = capturarEnvio();
        assertTrue(enviada.anexos().isEmpty());
        assertTrue(enviada.texto().contains("Não foi possível anexar: sumido.pdf"));
    }

    @Test
    void mensagemIncompletaNaoChegaAoSmtp() {
        OrcamentoEmailMensagem semLink = new OrcamentoEmailMensagem(
                "cliente@teste.com", "Assunto", null,
                new OrcamentoEmailMensagem.Orcamento("ORC-2026-ABCDEF", 1, BigDecimal.TEN, " ", null),
                null, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.enviar(semLink));
        assertThrows(IllegalArgumentException.class, () -> service.enviar(null));
        verify(emailService, never()).enviar(any());
    }
}
