package com.stentio.core.service;

import com.stentio.core.dto.CertificacaoResponse;
import com.stentio.core.exception.CertificacaoDeOutroRecursoException;
import com.stentio.core.exception.CertificacaoNaoEncontradaException;
import com.stentio.core.exception.ParNaoAtendidoException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.RecursoCertificacaoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * US-024 CA.1 e RN.2: o profissional comprova cada par que atende.
 *
 * <p>Tudo parte do recurso do token, nunca de um id vindo na requisição — é o que faz a
 * RN.8 valer sem depender de o front se comportar.
 */
@Service
public class CertificacaoRecursoService {

    private final RecursoCertificacaoRepository certificacoes;
    private final RecursoAutenticadoService recursoAutenticado;
    private final IdiomaRepository idiomas;
    private final ArquivoStorageService storage;
    private final Clock clock;

    public CertificacaoRecursoService(RecursoCertificacaoRepository certificacoes,
                                      RecursoAutenticadoService recursoAutenticado,
                                      IdiomaRepository idiomas,
                                      ArquivoStorageService storage,
                                      Clock clock) {
        this.certificacoes = certificacoes;
        this.recursoAutenticado = recursoAutenticado;
        this.idiomas = idiomas;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public CertificacaoResponse enviar(UUID idiomaOrigemId, UUID idiomaDestinoId,
                                       MultipartFile arquivo, UsuarioAutenticado ator) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new SolicitacaoInvalidaException("Envie o documento da certificação");
        }
        if (idiomaOrigemId == null || idiomaDestinoId == null) {
            throw new SolicitacaoInvalidaException("Informe o par de idiomas da certificação");
        }
        if (idiomaOrigemId.equals(idiomaDestinoId)) {
            throw new SolicitacaoInvalidaException("O idioma de origem e o de destino devem ser diferentes");
        }

        Recurso recurso = recursoAutenticado.resolver(ator);

        // RN.3: certificar um par que o recurso não declarou atender não significa nada —
        // ele nunca receberia etapa desse par de qualquer forma.
        if (!recurso.atendePar(idiomaOrigemId, idiomaDestinoId)) {
            throw new ParNaoAtendidoException(
                    "Este par de idiomas não está no seu cadastro. Acrescente o preço do par antes de certificá-lo");
        }

        Optional<RecursoCertificacao> existente = certificacoes
                .findByRecursoIdAndIdiomaOrigemIdAndIdiomaDestinoId(
                        recurso.getId(), idiomaOrigemId, idiomaDestinoId);

        Instant agora = clock.instant();
        ArquivoArmazenado salvo = storage.salvarTodos(List.of(arquivo)).getFirst();

        RecursoCertificacao certificacao;
        String caminhoSubstituido = null;
        try {
            if (existente.isPresent()) {
                certificacao = existente.get();
                // Lança quando já está aprovada (RN.2), antes de qualquer gravação valer.
                caminhoSubstituido = certificacao.reenviar(salvo.nomeOriginal(), salvo.caminho(), agora);
            } else {
                certificacao = new RecursoCertificacao(
                        recurso, carregar(idiomaOrigemId), carregar(idiomaDestinoId),
                        salvo.nomeOriginal(), salvo.caminho(), agora);
            }
            certificacoes.save(certificacao);
        } catch (RuntimeException ex) {
            // O arquivo já está no disco e o banco não vai registrá-lo.
            storage.remover(List.of(salvo.caminho()));
            throw ex;
        }

        // Só depois de a gravação passar. Apagar antes deixaria a certificação apontando
        // para um arquivo que não existe mais se a transição falhasse no meio.
        if (caminhoSubstituido != null && !caminhoSubstituido.equals(salvo.caminho())) {
            storage.remover(List.of(caminhoSubstituido));
        }

        return CertificacaoResponse.de(certificacao);
    }

    /** CA.5: o profissional acompanha o que enviou, com o motivo de cada recusa. */
    @Transactional(readOnly = true)
    public List<CertificacaoResponse> minhasCertificacoes(UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        return certificacoes.listarDoRecurso(recurso.getId()).stream()
                .map(CertificacaoResponse::de)
                .toList();
    }

    /**
     * US-024 RN.8: o profissional baixa o documento dele, e o id não basta como permissão.
     *
     * <p>A conferência é de dono, contra o recurso do token — um id de certificação de
     * outra pessoa devolve 403, não o arquivo.
     */
    @Transactional(readOnly = true)
    public DocumentoCertificacao localizarMeuDocumento(UUID certificacaoId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        RecursoCertificacao certificacao = certificacoes.buscarComRecurso(certificacaoId)
                .orElseThrow(() -> new CertificacaoNaoEncontradaException(certificacaoId));

        if (!certificacao.pertenceA(recurso.getId())) {
            throw new CertificacaoDeOutroRecursoException("Esta certificação não é sua");
        }
        return new DocumentoCertificacao(certificacao.getArquivoNome(), certificacao.getArquivoCaminho());
    }

    /** O que o download precisa, já conferido. */
    public record DocumentoCertificacao(String nomeOriginal, String caminho) {
    }

    /**
     * Carrega de verdade em vez de usar uma referência lazy: a resposta mostra o nome e o
     * código do idioma logo em seguida, então o proxy seria inicializado de qualquer jeito.
     */
    private Idioma carregar(UUID idiomaId) {
        return idiomas.findById(idiomaId)
                .orElseThrow(() -> new SolicitacaoInvalidaException("Idioma não encontrado: " + idiomaId));
    }
}
