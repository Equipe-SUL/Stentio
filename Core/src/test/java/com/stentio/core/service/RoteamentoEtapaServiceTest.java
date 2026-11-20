package com.stentio.core.service;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoteamentoEtapaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");

    private final AvancoEtapaService avanco = mock(AvancoEtapaService.class);
    private final DisparoEtapaService disparo = mock(DisparoEtapaService.class);
    private final ConviteEtapaEmailService convites = mock(ConviteEtapaEmailService.class);
    private final RoteamentoEtapaService service = new RoteamentoEtapaService(
            avanco, disparo, convites, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void disparaAutomaticamenteEtapaLiberadaSemRecurso() {
        EtapaOrdemServico entregue = mock(EtapaOrdemServico.class);
        EtapaOrdemServico proxima = mock(EtapaOrdemServico.class);
        var resultadoAvanco = new AvancoEtapaService.Resultado(Optional.of(proxima), true);
        when(avanco.avancar(entregue)).thenReturn(resultadoAvanco);
        when(disparo.dispararAutomaticamente(proxima)).thenReturn(Optional.of(
                new DisparoEtapaResponse(UUID.randomUUID(), StatusEtapaOrdemServico.DISPARADA, 3)));

        RoteamentoEtapaService.Resultado resultado = service.rotear(entregue);

        assertSame(proxima, resultado.proximaEtapa().orElseThrow());
        assertTrue(resultado.conviteAutomatico());
        assertEquals(3, resultado.recursosNotificados());
        verify(convites, never()).avisarEtapaLiberada(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void semElegiveisMantemEtapaPendenteSemConvite() {
        EtapaOrdemServico entregue = mock(EtapaOrdemServico.class);
        EtapaOrdemServico proxima = mock(EtapaOrdemServico.class);
        when(avanco.avancar(entregue))
                .thenReturn(new AvancoEtapaService.Resultado(Optional.of(proxima), true));
        when(disparo.dispararAutomaticamente(proxima)).thenReturn(Optional.empty());

        RoteamentoEtapaService.Resultado resultado = service.rotear(entregue);

        assertSame(proxima, resultado.proximaEtapa().orElseThrow());
        assertFalse(resultado.conviteAutomatico());
        assertEquals(0, resultado.recursosNotificados());
        verify(convites, never()).avisarEtapaLiberada(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void etapaAlocadaOuAceitaNaoAbreConviteAutomatico() {
        for (StatusEtapaOrdemServico status : List.of(
                StatusEtapaOrdemServico.ALOCADO, StatusEtapaOrdemServico.ACEITA)) {
            EtapaOrdemServico entregue = mock(EtapaOrdemServico.class);
            EtapaOrdemServico proxima = mock(EtapaOrdemServico.class);
            Recurso recurso = mock(Recurso.class);
            when(proxima.getStatus()).thenReturn(status);
            when(proxima.getRecurso()).thenReturn(recurso);
            when(avanco.avancar(entregue))
                    .thenReturn(new AvancoEtapaService.Resultado(Optional.of(proxima), false));

            RoteamentoEtapaService.Resultado resultado = service.rotear(entregue);

            assertSame(proxima, resultado.proximaEtapa().orElseThrow());
            assertFalse(resultado.conviteAutomatico());
            assertEquals(1, resultado.recursosNotificados());
            verify(convites).avisarEtapaLiberada(proxima, recurso, AGORA);
        }
        verify(disparo, never()).dispararAutomaticamente(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ultimaEtapaNaoFazRoteamento() {
        EtapaOrdemServico entregue = mock(EtapaOrdemServico.class);
        when(avanco.avancar(entregue))
                .thenReturn(new AvancoEtapaService.Resultado(Optional.empty(), false));

        RoteamentoEtapaService.Resultado resultado = service.rotear(entregue);

        assertTrue(resultado.proximaEtapa().isEmpty());
        assertFalse(resultado.conviteAutomatico());
        assertEquals(0, resultado.recursosNotificados());
        verify(disparo, never()).dispararAutomaticamente(org.mockito.ArgumentMatchers.any());
        verify(convites, never()).avisarEtapaLiberada(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
