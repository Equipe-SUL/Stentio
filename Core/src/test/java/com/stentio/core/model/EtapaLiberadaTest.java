package com.stentio.core.model;

import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
 * US-014 T0 (CA.1 / RN.1): a regra de etapa liberada, sem banco e sem Spring.
 *
 * <p>Etapa 1 e etapa 2 da mesma OS. A etapa 2 só pode ser disparada, aceita por convite
 * aberto ou entregue depois que a etapa 1 for entregue.
 */
class EtapaLiberadaTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-27T12:00:00Z");

    private Recurso ana;
    private Recurso bruno;
    private EtapaOrdemServico etapa1;
    private EtapaOrdemServico etapa2;

    @BeforeEach
    void preparar() {
        ana = recurso();
        bruno = recurso();
        OrdemServico ordem = mock(OrdemServico.class);
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Tradução");
        etapa1 = new EtapaOrdemServico(ordem, 1, "Tradução", tipo, 24);
        etapa2 = new EtapaOrdemServico(ordem, 2, "Revisão", tipo, 12);
    }

    private static Recurso recurso() {
        Recurso r = mock(Recurso.class);
        when(r.getId()).thenReturn(UUID.randomUUID());
        when(r.elegivelParaAlocacao()).thenReturn(true);
        return r;
    }

    private void entregarEtapa1() {
        etapa1.disparar(null, AGORA);
        etapa1.aceitarPor(null, ana, AGORA);
        etapa1.entregar(null, "etapa1.docx", "2026/11/etapa1.docx", AGORA);
    }

    // ------------------------------------------------------------------ liberada()

    @Test
    @DisplayName("RN.1: a primeira etapa da sequência está sempre liberada")
    void primeiraSempreLiberada() {
        assertTrue(etapa1.liberada(null));
    }

    @Test
    @DisplayName("RN.1: a etapa 2 não está liberada enquanto a etapa 1 não for entregue")
    void segundaNaoLiberadaAntesDaEntrega() {
        assertFalse(etapa2.liberada(etapa1));

        etapa1.disparar(null, AGORA);
        etapa1.aceitarPor(null, ana, AGORA);
        assertFalse(etapa2.liberada(etapa1), "aceita ainda não é entregue");
    }

    @Test
    @DisplayName("RN.1: a etapa 2 fica liberada quando a etapa 1 é entregue")
    void segundaLiberadaDepoisDaEntrega() {
        entregarEtapa1();

        assertTrue(etapa2.liberada(etapa1));
    }

    @Test
    @DisplayName("Sem a anterior, uma etapa que não é a primeira não está liberada")
    void semAnteriorNaoLibera() {
        assertFalse(etapa2.liberada(null));
    }

    @Test
    @DisplayName("Passar uma etapa que não é a imediatamente anterior é erro de programação")
    void anteriorErrada() {
        TipoServico tipo = etapa1.getTipoServico();
        EtapaOrdemServico etapa3 = new EtapaOrdemServico(mock(OrdemServico.class), 3, "Formatação", tipo, 4);

        assertThrows(IllegalArgumentException.class, () -> etapa3.liberada(etapa1));
    }

    // ------------------------------------------------------------------ Cenário 1

    @Test
    @DisplayName("Cenário 1: disparar a etapa 2 antes da etapa 1 é recusado e nada muda")
    void naoDisparaForaDeOrdem() {
        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapa2.disparar(etapa1, AGORA));

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, etapa2.getStatus());
        assertNull(etapa2.getDisparadaEm());
    }

    @Test
    @DisplayName("Cenário 1: entregar a etapa 2, aceita com antecedência, antes da etapa 1 é recusado")
    void naoEntregaForaDeOrdem() {
        etapa2.alocar(bruno, PRAZO, AGORA);
        etapa2.aceitarPor(etapa1, bruno, AGORA);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> etapa2.entregar(etapa1, "etapa2.docx", "2026/11/etapa2.docx", AGORA));

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa2.getStatus());
        assertNull(etapa2.getArquivoEntregueCaminho());
        assertNull(etapa2.getEntregueEm());
    }

    @Test
    @DisplayName("Depois que a etapa 1 é entregue, a etapa 2 dispara e entrega normalmente")
    void seguindoAOrdemFunciona() {
        entregarEtapa1();

        etapa2.disparar(etapa1, AGORA);
        etapa2.aceitarPor(etapa1, bruno, AGORA);
        etapa2.entregar(etapa1, "etapa2.docx", "2026/11/etapa2.docx", AGORA);

        assertEquals(StatusEtapaOrdemServico.ENTREGUE, etapa2.getStatus());
    }

    @Test
    @DisplayName("CA.1: convite aberto de etapa não liberada não pode ser aceito")
    void naoAceitaConviteAbertoForaDeOrdem() {
        // Estado que a develop deixava criar antes desta correção: etapa 2 disparada com a
        // etapa 1 em andamento. O aceite tem de barrar mesmo assim.
        ReflectionTestUtils.setField(etapa2, "status", StatusEtapaOrdemServico.DISPARADA);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> etapa2.aceitarPor(etapa1, bruno, AGORA));
        assertNull(etapa2.getRecurso());
    }

    // ------------------------------------------------------------------ o que continua permitido

    @Test
    @DisplayName("CA.1: alocar com antecedência continua permitido")
    void alocaComAntecedencia() {
        etapa2.alocar(bruno, PRAZO, AGORA);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, etapa2.getStatus());
        // A ordem não se perde: a etapa continua não liberada, mesmo com o status ALOCADO.
        assertFalse(etapa2.liberada(etapa1));
    }

    @Test
    @DisplayName("CA.1: aceitar convite nominal com antecedência continua permitido")
    void aceitaConviteNominalComAntecedencia() {
        etapa2.alocar(bruno, PRAZO, AGORA);

        etapa2.aceitarPor(etapa1, bruno, AGORA);

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa2.getStatus());
        assertTrue(etapa2.pertenceA(bruno));
    }

    // ------------------------------------------------------------------ recusa

    @Test
    @DisplayName("RN.1: recusa de etapa não liberada volta para AGUARDANDO_ETAPA_ANTERIOR")
    void recusaNaoLiberadaVoltaAAguardar() {
        etapa2.alocar(bruno, PRAZO, AGORA);

        etapa2.recusar(etapa1);

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, etapa2.getStatus());
        assertNull(etapa2.getRecurso());
        assertNull(etapa2.getDataLimiteEntrega());
    }

    @Test
    @DisplayName("RN.1: recusa de etapa liberada volta para PENDENTE")
    void recusaLiberadaVoltaAPendente() {
        entregarEtapa1();
        etapa2.alocar(bruno, PRAZO, AGORA);

        etapa2.recusar(etapa1);

        assertEquals(StatusEtapaOrdemServico.PENDENTE, etapa2.getStatus());
    }
}
