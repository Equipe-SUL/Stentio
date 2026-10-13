package com.stentio.core.service;

import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.ArquivoArmazenado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArquivoStorageServiceTest {

    @TempDir
    Path diretorio;

    private ArquivoStorageService storage() {
        return new ArquivoStorageService(diretorio.toString(), "pdf,png", 2);
    }

    private MultipartFile arquivo(String nome, String conteudo) {
        return new MockMultipartFile("arquivos", nome, "application/pdf", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("CA.3: deve gravar o arquivo em disco e devolver o caminho persistido no banco")
    void deveGravarArquivoEDevolverCaminho() {
        List<ArquivoArmazenado> salvos = storage().salvarTodos(List.of(arquivo("documento.pdf", "conteudo")));

        assertEquals(1, salvos.size());
        ArquivoArmazenado salvo = salvos.getFirst();

        assertEquals("documento.pdf", salvo.nomeOriginal());
        assertEquals(8L, salvo.tamanhoBytes());
        assertTrue(Files.exists(diretorio.resolve(salvo.caminho())), "o arquivo deve existir em disco");
        // O nome em disco é aleatório: dois envios do mesmo nome não se sobrescrevem.
        assertFalse(salvo.caminho().contains("documento.pdf"), "o nome do usuário não vai para o disco");
        assertTrue(salvo.caminho().endsWith(".pdf"), "a extensão é preservada");
    }

    @Test
    @DisplayName("Deve gravar dentro do diretório base mesmo quando o nome enviado tenta sair dele")
    void deveIgnorarCaminhoNoNomeEnviado() {
        List<ArquivoArmazenado> salvos = storage().salvarTodos(List.of(arquivo("../../../etc/invasor.pdf", "x")));

        ArquivoArmazenado salvo = salvos.getFirst();
        assertEquals("invasor.pdf", salvo.nomeOriginal(), "o nome guardado perde qualquer diretório");
        assertFalse(salvo.caminho().contains(".."), "o caminho não escapa do diretório base");
        assertTrue(Files.exists(diretorio.resolve(salvo.caminho())));
    }

    @Test
    @DisplayName("Deve recusar extensão fora da lista permitida")
    void deveRecusarExtensaoNaoPermitida() {
        SolicitacaoInvalidaException erro = assertThrows(SolicitacaoInvalidaException.class,
                () -> storage().salvarTodos(List.of(arquivo("script.exe", "x"))));

        assertTrue(erro.getMessage().contains("exe"));
    }

    @Test
    @DisplayName("Deve recusar arquivo vazio")
    void deveRecusarArquivoVazio() {
        assertThrows(SolicitacaoInvalidaException.class,
                () -> storage().salvarTodos(List.of(arquivo("documento.pdf", ""))));
    }

    @Test
    @DisplayName("Deve recusar mais arquivos que o limite configurado")
    void deveRecusarAcimaDoLimite() {
        List<MultipartFile> tres = List.of(
                arquivo("a.pdf", "x"), arquivo("b.pdf", "y"), arquivo("c.pdf", "z"));

        assertThrows(SolicitacaoInvalidaException.class, () -> storage().salvarTodos(tres));
    }

    @Test
    @DisplayName("Deve apagar os arquivos já gravados quando um dos seguintes falha")
    void deveLimparQuandoUmArquivoFalha() {
        ArquivoStorageService storage = storage();
        List<MultipartFile> arquivos = List.of(arquivo("valido.pdf", "x"), arquivo("invalido.exe", "y"));

        assertThrows(SolicitacaoInvalidaException.class, () -> storage.salvarTodos(arquivos));

        try (var conteudo = Files.walk(diretorio)) {
            assertTrue(conteudo.noneMatch(Files::isRegularFile), "nenhum arquivo pode sobrar em disco");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @DisplayName("RN.3: lista vazia de arquivos é aceita")
    void deveAceitarListaVazia() {
        assertTrue(storage().salvarTodos(List.of()).isEmpty());
        assertTrue(storage().salvarTodos(null).isEmpty());
    }
}
