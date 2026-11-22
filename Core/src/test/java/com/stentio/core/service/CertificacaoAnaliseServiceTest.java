package com.stentio.core.service;

import com.stentio.core.dto.CertificacaoResponse;
import com.stentio.core.exception.CertificacaoInvalidaException;
import com.stentio.core.exception.CertificacaoNaoEncontradaException;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.model.StatusCertificacao;
import com.stentio.core.repository.RecursoCertificacaoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-024 CA.2 e RN.4: o que a análise grava, e o que ela recusa. */
class CertificacaoAnaliseServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T15:00:00Z");

    private final RecursoCertificacaoRepository certificacoes = mock(RecursoCertificacaoRepository.class);
    private final CertificacaoRejeicaoEmailService avisos = mock(CertificacaoRejeicaoEmailService.class);

    private final CertificacaoAnaliseService service = new CertificacaoAnaliseService(
            certificacoes, avisos, Clock.fixed(AGORA, ZoneOffset.UTC));

    private final UUID certificacaoId = UUID.randomUUID();
    private final UsuarioAutenticado gestor =
            new UsuarioAutenticado("gestor@aliancatraducoes.com", UUID.randomUUID(), false);

    private RecursoCertificacao certificacao;

    @BeforeEach
    void preparar() {
        Recurso recurso = mock(Recurso.class);
        when(recurso.getId()).thenReturn(UUID.randomUUID());
        when(recurso.getNome()).thenReturn("Ana Silva");

        certificacao = new RecursoCertificacao(
                recurso, idioma("Inglês", "en"), idioma("Português", "pt"),
                "diploma.pdf", "2026/11/diploma.pdf", Instant.parse("2026-11-20T10:00:00Z"));
        ReflectionTestUtils.setField(certificacao, "id", certificacaoId);

        when(certificacoes.buscarComRecurso(certificacaoId)).thenReturn(Optional.of(certificacao));
    }

    private static Idioma idioma(String nome, String iso) {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        when(i.getNome()).thenReturn(nome);
        when(i.getCodigoIso()).thenReturn(iso);
        return i;
    }

    @Test
    @DisplayName("Cenário 2: aprovar deixa a certificação aprovada, com quem analisou")
    void aprovar() {
        CertificacaoResponse resposta = service.aprovar(certificacaoId, gestor);

        assertEquals(StatusCertificacao.APROVADA, resposta.status());
        assertEquals(AGORA, certificacao.getAnalisadoEm());
        assertEquals("gestor@aliancatraducoes.com", certificacao.getAnalisadoPor());
        // Aprovação não avisa ninguém: a boa notícia aparece no portal.
        verify(avisos, never()).avisarRejeicao(any(), any(), any());
    }

    @Test
    @DisplayName("Cenário 3: rejeitar guarda o motivo e avisa o profissional")
    void rejeitarAvisa() {
        CertificacaoResponse resposta = service.rejeitar(certificacaoId, "Documento ilegível", gestor);

        assertEquals(StatusCertificacao.REJEITADA, resposta.status());
        assertEquals("Documento ilegível", resposta.motivoRejeicao());
        verify(avisos).avisarRejeicao(certificacao, "Documento ilegível", AGORA);
    }

    @Test
    @DisplayName("RN.4: certificação já analisada não é analisada de novo, e ninguém é avisado")
    void naoAnalisaDuasVezes() {
        certificacao.aprovar("outro@aliancatraducoes.com", AGORA);

        assertThrows(CertificacaoInvalidaException.class,
                () -> service.rejeitar(certificacaoId, "mudei de ideia", gestor));
        verify(avisos, never()).avisarRejeicao(any(), any(), any());
    }

    @Test
    @DisplayName("certificação inexistente é 404")
    void inexistente() {
        when(certificacoes.buscarComRecurso(certificacaoId)).thenReturn(Optional.empty());

        assertThrows(CertificacaoNaoEncontradaException.class,
                () -> service.aprovar(certificacaoId, gestor));
    }
}
