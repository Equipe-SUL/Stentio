package com.stentio.core.service;

import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.ArquivoArmazenado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CA.3: grava o arquivo em disco e devolve o caminho que será persistido no banco.
 * O arquivo nunca é gravado com o nome enviado pelo usuário: um nome como "../../app.jar"
 * sobrescreveria arquivos do servidor, então o nome em disco é sempre um UUID.
 */
@Service
public class ArquivoStorageService {

    private static final DateTimeFormatter PASTA_POR_MES = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final int TAMANHO_MAXIMO_NOME = 255;

    private final Path diretorioBase;
    private final Set<String> extensoesPermitidas;
    private final int maximoArquivos;

    public ArquivoStorageService(
            @Value("${app.arquivos.diretorio:uploads}") String diretorio,
            @Value("${app.arquivos.extensoes-permitidas}") String extensoes,
            @Value("${app.arquivos.maximo-por-solicitacao:10}") int maximoArquivos) {

        this.diretorioBase = Paths.get(diretorio).toAbsolutePath().normalize();
        this.extensoesPermitidas = Arrays.stream(extensoes.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.maximoArquivos = maximoArquivos;

        try {
            Files.createDirectories(diretorioBase);
        } catch (IOException ex) {
            // Falha no boot é melhor que falha na primeira requisição.
            throw new UncheckedIOException("Não foi possível criar o diretório de arquivos: " + diretorioBase, ex);
        }
    }

    public List<ArquivoArmazenado> salvarTodos(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            return List.of();
        }
        if (arquivos.size() > maximoArquivos) {
            throw new SolicitacaoInvalidaException("Envie no máximo " + maximoArquivos + " arquivos por solicitação");
        }

        List<ArquivoArmazenado> salvos = new ArrayList<>();
        try {
            for (MultipartFile arquivo : arquivos) {
                salvos.add(salvar(arquivo));
            }
        } catch (RuntimeException ex) {
            // Se o terceiro arquivo falhar, os dois primeiros não podem ficar órfãos no disco.
            remover(salvos.stream().map(ArquivoArmazenado::caminho).toList());
            throw ex;
        }
        return salvos;
    }

    public Resource carregar(String caminho) {
        try {
            Path arquivo = resolverDentroDaBase(caminho);
            Resource recurso = new UrlResource(arquivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new SolicitacaoInvalidaException("Arquivo não está mais disponível no servidor");
            }
            return recurso;
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao ler o arquivo " + caminho, ex);
        }
    }

    // Usado para desfazer a gravação quando a transação do banco não se conclui.
    public void remover(Collection<String> caminhos) {
        for (String caminho : caminhos) {
            try {
                Files.deleteIfExists(resolverDentroDaBase(caminho));
            } catch (IOException | RuntimeException ex) {
                // Um arquivo órfão não pode derrubar a requisição que já falhou por outro motivo.
            }
        }
    }

    private ArquivoArmazenado salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new SolicitacaoInvalidaException("Arquivo enviado está vazio");
        }

        String nomeOriginal = sanitizarNome(arquivo.getOriginalFilename());
        String extensao = extrairExtensao(nomeOriginal);

        if (!extensoesPermitidas.contains(extensao)) {
            throw new SolicitacaoInvalidaException(
                    "Extensão de arquivo não permitida: " + (extensao.isEmpty() ? "(sem extensão)" : extensao));
        }

        String caminhoRelativo = LocalDate.now().format(PASTA_POR_MES) + "/" + UUID.randomUUID() + "." + extensao;
        Path destino = resolverDentroDaBase(caminhoRelativo);

        try {
            Files.createDirectories(destino.getParent());
            try (InputStream entrada = arquivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gravar o arquivo " + nomeOriginal, ex);
        }

        return new ArquivoArmazenado(nomeOriginal, caminhoRelativo, arquivo.getContentType(), arquivo.getSize());
    }

    // Garante que o caminho final fique dentro do diretório base, mesmo que contenha ".." ou barra inicial.
    private Path resolverDentroDaBase(String caminhoRelativo) {
        Path resolvido = diretorioBase.resolve(caminhoRelativo).normalize();
        if (!resolvido.startsWith(diretorioBase)) {
            throw new SolicitacaoInvalidaException("Caminho de arquivo inválido");
        }
        return resolvido;
    }

    // O nome original volta na resposta e vai para o cabeçalho de download, então perde qualquer
    // separador de diretório e caractere de controle antes de ser guardado.
    private static String sanitizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "arquivo";
        }
        String limpo = nome.replace('\\', '/');
        limpo = limpo.substring(limpo.lastIndexOf('/') + 1);
        limpo = limpo.replaceAll("[\\p{Cntrl}\"]", "").strip();

        if (limpo.isBlank() || limpo.equals(".") || limpo.equals("..")) {
            return "arquivo";
        }
        return limpo.length() > TAMANHO_MAXIMO_NOME ? limpo.substring(0, TAMANHO_MAXIMO_NOME) : limpo;
    }

    private static String extrairExtensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        if (ponto < 0 || ponto == nome.length() - 1) {
            return "";
        }
        return nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }
}
