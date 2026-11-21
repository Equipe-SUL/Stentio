package com.stentio.core.model;

import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * US-013: a máquina de estados da etapa, sem banco e sem Spring.
 *
 * <p>É aqui que as regras moram, então é aqui que elas são verificadas. Os serviços
 * decidem quem pode chamar; o que é uma transição legal é desta classe.
 */
class EtapaOrdemServicoTransicoesTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-24T12:00:00Z");

    private Recurso ana;
    private Recurso bruno;
    private EtapaOrdemServico etapa;

    @BeforeEach
    void preparar() {
        ana = recursoComId();
        bruno = recursoComId();
        etapa = novaEtapa();
    }

    private static Recurso recursoComId() {
        Recurso r = mock(Recurso.class);
        when(r.getId()).thenReturn(UUID.randomUUID());
        // alocar() confere a RN.5 da US-023 antes de qualquer outra coisa.
        when(r.isAtivo()).thenReturn(true);
        return r;
    }

    private static EtapaOrdemServico novaEtapa() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Tradução");
        return new EtapaOrdemServico(mock(OrdemServico.class), 1, "Tradução Principal", tipo, 24);
    }

    @Test
    @DisplayName("alocar deixa a etapa ALOCADO, esperando o aceite da pessoa escolhida")
    void alocarEsperaAceite() {
        etapa.alocar(ana, PRAZO, AGORA, true);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, etapa.getStatus());
        assertTrue(etapa.pertenceA(ana));
        assertFalse(etapa.pertenceA(bruno));
        // US-012 RN.3: alocado ainda não é iniciado, então a troca continua liberada.
        assertFalse(etapa.iniciada());
    }

    @Test
    @DisplayName("a primeira etapa da sequência está liberada")
    void primeiraEtapaEstaLiberada() {
        assertTrue(etapa.liberada(null));
    }

    @Test
    @DisplayName("uma etapa posterior só fica liberada depois da entrega da anterior")
    void etapaPosteriorDependeDaEntregaAnterior() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");
        EtapaOrdemServico posterior =
                new EtapaOrdemServico(mock(OrdemServico.class), 2, "Revisão", tipo, 12);

        assertFalse(posterior.liberada(null));
        assertFalse(posterior.liberada(StatusEtapaOrdemServico.PENDENTE));
        assertTrue(posterior.liberada(StatusEtapaOrdemServico.ENTREGUE));
    }

    @Test
    @DisplayName("disparar abre a etapa e descarta a alocação nominal anterior")
    void dispararDescartaAlocacao() {
        etapa.alocar(ana, PRAZO, AGORA, true);

        etapa.disparar(AGORA, true);

        assertEquals(StatusEtapaOrdemServico.DISPARADA, etapa.getStatus());
        assertNull(etapa.getRecurso());
        assertEquals(AGORA, etapa.getDisparadaEm());
    }

    @Test
    @DisplayName("RN.6: etapa já aceita não volta para a fila")
    void naoDisparaEtapaAceita() {
        etapa.disparar(AGORA, true);
        etapa.aceitarPor(ana, AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapa.disparar(AGORA, true));
        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa.getStatus());
        assertTrue(etapa.pertenceA(ana));
    }

    @Test
    @DisplayName("etapa bloqueada pela anterior não pode ser disparada")
    void naoDisparaEtapaAguardandoEtapaAnterior() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");
        EtapaOrdemServico etapaBloqueada =
                new EtapaOrdemServico(mock(OrdemServico.class), 2, "Revisão", tipo, 12);

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, etapaBloqueada.getStatus());
        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapaBloqueada.disparar(AGORA, false));
    }

    @Test
    @DisplayName("etapa alocada antecipadamente ainda não pode ser disparada")
    void naoDisparaAlocacaoAntecipada() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");
        EtapaOrdemServico posterior =
                new EtapaOrdemServico(mock(OrdemServico.class), 2, "Revisão", tipo, 12);
        posterior.alocar(ana, PRAZO, AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> posterior.disparar(AGORA, false));
        assertEquals(StatusEtapaOrdemServico.ALOCADO, posterior.getStatus());
    }

    @Test
    @DisplayName("uma etapa já disparada não envia convites duplicados")
    void naoDisparaEtapaJaDisparada() {
        etapa.disparar(AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapa.disparar(AGORA, true));
    }

    @Test
    @DisplayName("aceitar marca o dono, o instante e congela a etapa para troca")
    void aceitarCongelaAEtapa() {
        etapa.disparar(AGORA, true);

        etapa.aceitarPor(bruno, AGORA, true);

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa.getStatus());
        assertEquals(AGORA, etapa.getAceitoEm());
        assertTrue(etapa.pertenceA(bruno));
        // US-012 RN.3: a partir daqui a troca exige administrador.
        assertTrue(etapa.iniciada());
    }

    @Test
    @DisplayName("RN.5: recusar devolve a etapa a PENDENTE, sem dono e sem prazo")
    void recusarLiberaAEtapa() {
        etapa.alocar(ana, PRAZO, AGORA, true);

        etapa.recusar(true);

        assertEquals(StatusEtapaOrdemServico.PENDENTE, etapa.getStatus());
        assertNull(etapa.getRecurso());
        assertNull(etapa.getDataLimiteEntrega());
    }

    @Test
    @DisplayName("recusar alocação antecipada mantém a etapa bloqueada")
    void recusarEtapaAindaBloqueada() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");
        EtapaOrdemServico posterior =
                new EtapaOrdemServico(mock(OrdemServico.class), 2, "Revisão", tipo, 12);
        posterior.alocar(ana, PRAZO, AGORA, true);

        posterior.recusar(false);

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, posterior.getStatus());
        assertNull(posterior.getRecurso());
        assertNull(posterior.getDataLimiteEntrega());
    }

    @Test
    @DisplayName("só se recusa um convite nominal: etapa disparada não tem o que recusar")
    void naoRecusaEtapaDisparada() {
        etapa.disparar(AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapa.recusar(true));
    }

    @Test
    @DisplayName("CA.5: entregar guarda nome e caminho e fecha a etapa")
    void entregarFechaAEtapa() {
        etapa.disparar(AGORA, true);
        etapa.aceitarPor(ana, AGORA, true);

        etapa.entregar("traducao-final.docx", "2026/11/abc.docx", AGORA, true);

        assertEquals(StatusEtapaOrdemServico.ENTREGUE, etapa.getStatus());
        assertEquals("traducao-final.docx", etapa.getArquivoEntregueNome());
        assertEquals("2026/11/abc.docx", etapa.getArquivoEntregueCaminho());
        assertEquals(AGORA, etapa.getEntregueEm());
    }

    @Test
    @DisplayName("RN.4: não se entrega uma etapa que ninguém assumiu")
    void naoEntregaEtapaNaoAceita() {
        etapa.disparar(AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> etapa.entregar("x.docx", "2026/11/x.docx", AGORA, true));
        assertNull(etapa.getArquivoEntregueCaminho());
    }

    @Test
    @DisplayName("etapa aceita com antecedência não pode ser entregue antes da liberação")
    void naoEntregaEtapaAceitaAindaBloqueada() {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");
        EtapaOrdemServico posterior =
                new EtapaOrdemServico(mock(OrdemServico.class), 2, "Revisão", tipo, 12);
        posterior.alocar(ana, PRAZO, AGORA, true);
        posterior.aceitarPor(ana, AGORA, false);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> posterior.entregar("x.docx", "2026/11/x.docx", AGORA, false));
        assertNull(posterior.getArquivoEntregueCaminho());
    }

    @Test
    @DisplayName("aceite nominal pode acontecer antes da liberação")
    void aceitaConviteNominalAntecipado() {
        etapa.alocar(ana, PRAZO, AGORA, true);

        etapa.aceitarPor(ana, AGORA, false);

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa.getStatus());
    }

    @Test
    @DisplayName("convite aberto exige etapa liberada para aceite")
    void naoAceitaConviteAbertoAntecipado() {
        etapa.disparar(AGORA, true);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> etapa.aceitarPor(ana, AGORA, false));
        assertEquals(StatusEtapaOrdemServico.DISPARADA, etapa.getStatus());
    }
}
