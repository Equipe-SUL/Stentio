package com.stentio.core.service;

import org.springframework.context.ApplicationEventPublisher;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecusaEtapaServiceTest {

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EtapaLiberacaoService liberacao = mock(EtapaLiberacaoService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final RecusaEtapaEmailService avisos = mock(RecusaEtapaEmailService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final RecusaEtapaService service = new RecusaEtapaService(
            etapas,
            liberacao,
            recursoAutenticado,
            avisos,
            historico,
            eventos,
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void recusaDeAlocacaoAntecipadaMantemEtapaBloqueada() {
        UUID etapaId = UUID.randomUUID();
        UsuarioAutenticado ator = new UsuarioAutenticado("recurso@teste.invalid", false);
        Recurso recurso = mock(Recurso.class);
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);

        when(recursoAutenticado.resolver(ator)).thenReturn(recurso);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(recurso)).thenReturn(true);
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR);
        when(etapa.getId()).thenReturn(etapaId);
        OrdemServico ordem = mock(OrdemServico.class);
        when(ordem.getId()).thenReturn(UUID.randomUUID());
        when(etapa.getOrdemServico()).thenReturn(ordem);
        when(liberacao.estaLiberada(etapa)).thenReturn(false);

        service.recusar(etapaId, ator);

        verify(etapa).recusar(false);
        verify(etapas).save(etapa);
        verify(historico).save(any());
        verify(avisos).avisarRecusa(etapa, recurso, Instant.parse("2026-11-20T12:00:00Z"));
    }
}
