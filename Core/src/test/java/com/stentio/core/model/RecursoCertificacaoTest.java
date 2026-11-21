package com.stentio.core.model;

import com.stentio.core.exception.CertificacaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** US-024: o ciclo da certificação, sem banco e sem Spring. */
class RecursoCertificacaoTest {

    private static final Instant ENVIO = Instant.parse("2026-11-21T10:00:00Z");
    private static final Instant ANALISE = Instant.parse("2026-11-21T15:00:00Z");
    private static final String GESTOR = "gestor@aliancatraducoes.com";

    private RecursoCertificacao certificacao;

    @BeforeEach
    void preparar() {
        certificacao = new RecursoCertificacao(
                mock(Recurso.class), mock(Idioma.class), mock(Idioma.class),
                "diploma.pdf", "2026/11/diploma.pdf", ENVIO);
    }

    @Test
    @DisplayName("CA.1: nasce pendente")
    void nascePendente() {
        assertEquals(StatusCertificacao.PENDENTE, certificacao.getStatus());
        assertEquals(ENVIO, certificacao.getEnviadoEm());
        assertNull(certificacao.getAnalisadoEm());
    }

    @Test
    @DisplayName("CA.2: aprovar registra quem analisou e quando")
    void aprovarRegistraAAnalise() {
        certificacao.aprovar(GESTOR, ANALISE);

        assertEquals(StatusCertificacao.APROVADA, certificacao.getStatus());
        assertEquals(GESTOR, certificacao.getAnalisadoPor());
        assertEquals(ANALISE, certificacao.getAnalisadoEm());
    }

    @Test
    @DisplayName("CA.2: rejeitar guarda o motivo")
    void rejeitarGuardaOMotivo() {
        certificacao.rejeitar("Documento ilegível", GESTOR, ANALISE);

        assertEquals(StatusCertificacao.REJEITADA, certificacao.getStatus());
        assertEquals("Documento ilegível", certificacao.getMotivoRejeicao());
    }

    @Test
    @DisplayName("RN.4: o que já foi analisado não é analisado de novo")
    void naoAnalisaDuasVezes() {
        certificacao.aprovar(GESTOR, ANALISE);

        assertThrows(CertificacaoInvalidaException.class, () -> certificacao.aprovar(GESTOR, ANALISE));
        assertThrows(CertificacaoInvalidaException.class,
                () -> certificacao.rejeitar("mudei de ideia", GESTOR, ANALISE));
    }

    @Test
    @DisplayName("RN.2: reenviar volta para pendente e limpa a recusa anterior")
    void reenviarLimpaARecusa() {
        certificacao.rejeitar("Documento ilegível", GESTOR, ANALISE);

        String anterior = certificacao.reenviar("diploma-v2.pdf", "2026/11/diploma-v2.pdf", ANALISE);

        assertEquals("2026/11/diploma.pdf", anterior);
        assertEquals(StatusCertificacao.PENDENTE, certificacao.getStatus());
        assertEquals("diploma-v2.pdf", certificacao.getArquivoNome());
        // O motivo antigo ao lado de um documento novo faria o profissional achar que a
        // recusa continua valendo.
        assertNull(certificacao.getMotivoRejeicao());
        assertNull(certificacao.getAnalisadoEm());
        assertNull(certificacao.getAnalisadoPor());
    }

    @Test
    @DisplayName("RN.2: certificação aprovada não é substituída pelo profissional")
    void naoSubstituiAprovada() {
        certificacao.aprovar(GESTOR, ANALISE);

        assertThrows(CertificacaoInvalidaException.class,
                () -> certificacao.reenviar("outro.pdf", "2026/11/outro.pdf", ANALISE));
        assertEquals("diploma.pdf", certificacao.getArquivoNome());
    }
}
