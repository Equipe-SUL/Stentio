package com.stentio.core.service;

import com.stentio.core.dto.CertificacaoPaginaResponse;
import com.stentio.core.dto.CertificacaoResponse;
import com.stentio.core.exception.CertificacaoNaoEncontradaException;
import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.model.StatusCertificacao;
import com.stentio.core.repository.RecursoCertificacaoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * US-024 CA.2: o gestor analisa cada documento.
 *
 * <p>A guarda de estado mora na entidade (RN.4): só o que está pendente é analisado, e
 * duas aprovações da mesma certificação não se sobrepõem em silêncio.
 */
@Service
public class CertificacaoAnaliseService {

    private final RecursoCertificacaoRepository certificacoes;
    private final CertificacaoRejeicaoEmailService avisos;
    private final Clock clock;

    public CertificacaoAnaliseService(RecursoCertificacaoRepository certificacoes,
                                      CertificacaoRejeicaoEmailService avisos,
                                      Clock clock) {
        this.certificacoes = certificacoes;
        this.avisos = avisos;
        this.clock = clock;
    }

    /** Mais antigas primeiro: quem esperou mais é analisado antes. */
    @Transactional(readOnly = true)
    public CertificacaoPaginaResponse listarParaAnalise(StatusCertificacao status, int pagina, int tamanho) {
        Page<RecursoCertificacao> resultado = certificacoes.listarParaAnalise(
                status,
                PageRequest.of(Math.max(pagina, 1) - 1, tamanho, Sort.by("enviadoEm").ascending()));
        return CertificacaoPaginaResponse.de(resultado);
    }

    @Transactional
    public CertificacaoResponse aprovar(UUID certificacaoId, UsuarioAutenticado ator) {
        RecursoCertificacao certificacao = buscar(certificacaoId);
        certificacao.aprovar(ator.email(), clock.instant());
        return CertificacaoResponse.de(certificacao);
    }

    @Transactional
    public CertificacaoResponse rejeitar(UUID certificacaoId, String motivo, UsuarioAutenticado ator) {
        RecursoCertificacao certificacao = buscar(certificacaoId);
        java.time.Instant agora = clock.instant();

        certificacao.rejeitar(motivo, ator.email(), agora);

        // CA.5: na mesma transação. Se a rejeição não valer, ninguém é avisado à toa.
        avisos.avisarRejeicao(certificacao, motivo, agora);

        return CertificacaoResponse.de(certificacao);
    }

    /** CA.2: o gestor precisa ver o documento para decidir. */
    @Transactional(readOnly = true)
    public CertificacaoRecursoService.DocumentoCertificacao localizarDocumento(UUID certificacaoId) {
        RecursoCertificacao certificacao = buscar(certificacaoId);
        return new CertificacaoRecursoService.DocumentoCertificacao(
                certificacao.getArquivoNome(), certificacao.getArquivoCaminho());
    }

    private RecursoCertificacao buscar(UUID certificacaoId) {
        return certificacoes.buscarComRecurso(certificacaoId)
                .orElseThrow(() -> new CertificacaoNaoEncontradaException(certificacaoId));
    }
}
