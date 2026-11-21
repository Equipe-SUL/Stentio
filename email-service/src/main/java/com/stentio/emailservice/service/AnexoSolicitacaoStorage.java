package com.stentio.emailservice.service;

import com.stentio.emailservice.dto.MensagemEmail;
import com.stentio.emailservice.dto.OrcamentoEmailMensagem;
import com.stentio.emailservice.exception.AnexoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * US-13: lê os anexos da solicitação no mesmo volume em que o Core gravou, montado aqui
 * como somente leitura. O email-service nunca escreve nem apaga nada nesse diretório.
 *
 * <p>Um anexo que não puder ser lido não derruba o e-mail: o orçamento precisa chegar ao
 * cliente de qualquer forma, e o corpo informa quais arquivos ficaram de fora.
 */
@Service
public class AnexoSolicitacaoStorage {

    private static final Logger log = LoggerFactory.getLogger(AnexoSolicitacaoStorage.class);
    private static final String TIPO_PADRAO = "application/octet-stream";

    private final Path diretorioBase;
    private final long limiteTotalBytes;

    public AnexoSolicitacaoStorage(
            @Value("${app.anexos.diretorio:uploads}") String diretorio,
            @Value("${app.anexos.limite-total-bytes:10485760}") long limiteTotalBytes) {
        this.diretorioBase = Paths.get(diretorio).toAbsolutePath().normalize();
        this.limiteTotalBytes = limiteTotalBytes;
    }

    /**
     * @return os anexos lidos e os nomes dos que ficaram de fora, na ordem recebida.
     */
    public Resultado carregar(List<OrcamentoEmailMensagem.Anexo> solicitados) {
        List<MensagemEmail.Anexo> carregados = new ArrayList<>();
        List<String> ignorados = new ArrayList<>();
        long total = 0;

        for (OrcamentoEmailMensagem.Anexo anexo : solicitados) {
            byte[] conteudo = ler(anexo);
            if (conteudo == null) {
                ignorados.add(anexo.nomeArquivo());
                continue;
            }
            // Um servidor SMTP recusa a mensagem inteira acima do seu limite, então o corte
            // é aqui: melhor o e-mail chegar com parte dos arquivos do que não chegar.
            if (total + conteudo.length > limiteTotalBytes) {
                log.warn("Anexo {} ficou fora do e-mail: limite total de {} bytes atingido",
                        anexo.nomeArquivo(), limiteTotalBytes);
                ignorados.add(anexo.nomeArquivo());
                continue;
            }
            total += conteudo.length;
            carregados.add(new MensagemEmail.Anexo(anexo.nomeArquivo(), conteudo, tipoDe(anexo)));
        }
        return new Resultado(List.copyOf(carregados), List.copyOf(ignorados));
    }

    private byte[] ler(OrcamentoEmailMensagem.Anexo anexo) {
        return anexo == null ? null : lerCaminho(anexo.caminho());
    }

    /** A leitura em si, sem saber de que fluxo veio o pedido. */
    private byte[] lerCaminho(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return null;
        }
        try {
            Path arquivo = resolverDentroDaBase(caminho);
            if (arquivo == null || !Files.isRegularFile(arquivo) || !Files.isReadable(arquivo)) {
                log.warn("Anexo {} não encontrado no volume compartilhado", caminho);
                return null;
            }
            return Files.readAllBytes(arquivo);
        } catch (IOException ex) {
            log.warn("Falha ao ler o anexo {}: {}", caminho, ex.getMessage());
            return null;
        }
    }

    /**
     * US-015 RN.5: o anexo único da entrega final, que não admite envio parcial.
     *
     * <p>Diferente de {@link #carregar(List)}, que pula o arquivo grande demais para o
     * e-mail do orçamento ainda chegar com o resto: aqui o arquivo é o conteúdo do e-mail.
     * Mandar a mensagem sem ele diria ao cliente que o trabalho está pronto e não entregaria
     * nada, então a falha é explícita e a mensagem volta para a fila.
     */
    public MensagemEmail.Anexo carregarObrigatorio(String nomeArquivo, String caminho, String tipoConteudo) {
        byte[] conteudo = lerCaminho(caminho);
        if (conteudo == null) {
            throw new AnexoIndisponivelException(
                    "Anexo " + nomeArquivo + " não pôde ser lido no volume compartilhado");
        }
        if (conteudo.length > limiteTotalBytes) {
            throw new AnexoIndisponivelException(
                    "Anexo " + nomeArquivo + " tem " + conteudo.length
                            + " bytes e o limite é " + limiteTotalBytes);
        }
        String tipo = tipoConteudo == null || tipoConteudo.isBlank() ? TIPO_PADRAO : tipoConteudo;
        return new MensagemEmail.Anexo(nomeArquivo, conteudo, tipo);
    }

    // O caminho vem de outro serviço pela fila, então é tratado como entrada não confiável:
    // ".." ou barra inicial não podem levar a leitura para fora do diretório de uploads.
    private Path resolverDentroDaBase(String caminhoRelativo) {
        Path resolvido = diretorioBase.resolve(caminhoRelativo).normalize();
        if (!resolvido.startsWith(diretorioBase)) {
            log.warn("Caminho de anexo recusado por sair do diretório base: {}", caminhoRelativo);
            return null;
        }
        return resolvido;
    }

    private static String tipoDe(OrcamentoEmailMensagem.Anexo anexo) {
        return anexo.tipoConteudo() == null || anexo.tipoConteudo().isBlank()
                ? TIPO_PADRAO
                : anexo.tipoConteudo();
    }

    public record Resultado(List<MensagemEmail.Anexo> carregados, List<String> ignorados) {
    }
}
