package com.stentio.core.model;

import com.stentio.core.exception.EtapasInvalidasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// US-011: as regras da sequência são domínio puro, então os testes não precisam de Spring.
class SequenciaEtapasTest {

    @Test
    @DisplayName("RN.1: aceita a sequência completa a partir de 1")
    void aceitaSequenciaCompleta() {
        assertDoesNotThrow(() -> SequenciaEtapas.validarOrdens(List.of(1, 2, 3)));
        // A ordem de chegada não importa: a tela pode enviar como o gestor arrastou.
        assertDoesNotThrow(() -> SequenciaEtapas.validarOrdens(List.of(3, 1, 2)));
    }

    @Test
    @DisplayName("RN.1: recusa números de ordem repetidos")
    void recusaOrdensRepetidas() {
        var erro = assertThrows(EtapasInvalidasException.class,
                () -> SequenciaEtapas.validarOrdens(List.of(1, 2, 2)));
        assertTrue(erro.getMessage().contains("repetidos"), erro.getMessage());
    }

    @Test
    @DisplayName("RN.1: recusa lacuna na numeração")
    void recusaLacuna() {
        var erro = assertThrows(EtapasInvalidasException.class,
                () -> SequenciaEtapas.validarOrdens(List.of(1, 3, 4)));
        assertTrue(erro.getMessage().contains("ordem 2"), erro.getMessage());
    }

    @Test
    @DisplayName("RN.1: recusa sequência que não começa em 1")
    void recusaInicioForaDoUm() {
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarOrdens(List.of(2, 3)));
    }

    @Test
    @DisplayName("RN.1: recusa lista vazia, nula ou com ordem nula")
    void recusaListaIncompleta() {
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarOrdens(List.of()));
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarOrdens(null));
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarOrdens(Arrays.asList(1, null)));
    }

    @Test
    @DisplayName("RN.2: o prazo em horas precisa ser maior que zero")
    void recusaPrazoInvalido() {
        assertDoesNotThrow(() -> SequenciaEtapas.validarPrazo(1));
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarPrazo(0));
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarPrazo(-5));
        assertThrows(EtapasInvalidasException.class, () -> SequenciaEtapas.validarPrazo(null));
    }

    @Test
    @DisplayName("CA.4: só a primeira etapa nasce pronta para trabalhar")
    void statusInicialSegueAOrdem() {
        assertEquals(StatusEtapaOrdemServico.PENDENTE, SequenciaEtapas.statusInicial(1));
        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, SequenciaEtapas.statusInicial(2));
        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, SequenciaEtapas.statusInicial(7));
    }
}
