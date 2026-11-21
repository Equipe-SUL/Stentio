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
    private final Clock clock;

    public CertificacaoAnaliseService(RecursoCertificacaoRepository certificacoes,
                                      Clock clock) {
        this.certificacoes = certificacoes;
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
        certificacao.rejeitar(motivo, ator.email(), clock.instant());
        return CertificacaoResponse.de(certificacao);
    }

    private RecursoCertificacao buscar(UUID certificacaoId) {
        return certificacoes.buscarComRecurso(certificacaoId)
                .orElseThrow(() -> new CertificacaoNaoEncontradaException(certificacaoId));
    }
}
