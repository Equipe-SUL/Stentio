package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// US-13: o caminho do anexo chega de outro serviço pela fila, então é entrada não confiável.
class AnexoSolicitacaoStorageTest {

    @TempDir
    Path base;

    private AnexoSolicitacaoStorage storage;

    @BeforeEach
    void preparar() throws IOException {
        Files.createDirectories(base.resolve("2026/10"));
        Files.writeString(base.resolve("2026/10/a.pdf"), "conteudo do contrato");
        storage = new AnexoSolicitacaoStorage(base.toString(), 10_485_760L);
    }

    private static OrcamentoEmailMensagem.Anexo anexo(String nome, String caminho) {
        return new OrcamentoEmailMensagem.Anexo(nome, caminho, "application/pdf");
    }

    @Test
    void carregaOArquivoGravadoPeloCore() {
        var resultado = storage.carregar(List.of(anexo("contrato.pdf", "2026/10/a.pdf")));

        assertEquals(1, resultado.carregados().size());
        assertTrue(resultado.ignorados().isEmpty());
        assertEquals("contrato.pdf", resultado.carregados().get(0).nome());
        assertEquals("conteudo do contrato",
                new String(resultado.carregados().get(0).conteudo(), StandardCharsets.UTF_8));
    }

    @Test
    void arquivoInexistenteEhIgnoradoSemDerrubarOEnvio() {
        var resultado = storage.carregar(List.of(
                anexo("contrato.pdf", "2026/10/a.pdf"),
                anexo("sumido.pdf", "2026/10/nao-existe.pdf")));

        assertEquals(1, resultado.carregados().size());
        assertEquals(List.of("sumido.pdf"), resultado.ignorados());
    }

    @Test
    void caminhoQueTentaSairDoDiretorioBaseEhRecusado() throws IOException {
        Files.writeString(base.getParent().resolve("segredo.txt"), "nao pode sair");

        var resultado = storage.carregar(List.of(anexo("segredo.txt", "../segredo.txt")));

        assertTrue(resultado.carregados().isEmpty());
        assertEquals(List.of("segredo.txt"), resultado.ignorados());
    }

    @Test
    void acimaDoLimiteTotalOsArquivosExcedentesFicamDeFora() throws IOException {
        Files.writeString(base.resolve("2026/10/b.pdf"), "segundo arquivo");
        // Limite menor que a soma dos dois: o primeiro entra, o segundo é avisado no corpo.
        var apertado = new AnexoSolicitacaoStorage(base.toString(), 25L);

        var resultado = apertado.carregar(List.of(
                anexo("a.pdf", "2026/10/a.pdf"),
                anexo("b.pdf", "2026/10/b.pdf")));

        assertEquals(List.of("a.pdf"), resultado.carregados().stream().map(a -> a.nome()).toList());
        assertEquals(List.of("b.pdf"), resultado.ignorados());
    }

    @Test
    void semAnexosDevolveListasVazias() {
        var resultado = storage.carregar(List.of());

        assertTrue(resultado.carregados().isEmpty());
        assertTrue(resultado.ignorados().isEmpty());
    }

    @Test
    void anexoSemCaminhoEhIgnorado() {
        var resultado = storage.carregar(List.of(anexo("sem-caminho.pdf", null)));

        assertEquals(List.of("sem-caminho.pdf"), resultado.ignorados());
    }
}
