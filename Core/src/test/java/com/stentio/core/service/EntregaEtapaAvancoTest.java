package com.stentio.core.service;

import org.springframework.context.ApplicationEventPublisher;
import com.stentio.core.dto.EntregaEtapaResponse;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntregaEtapaAvancoTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EtapaLiberacaoService liberacao = mock(EtapaLiberacaoService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final RoteamentoEtapaService roteamento = mock(RoteamentoEtapaService.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EntregaEtapaService service = new EntregaEtapaService(
            etapas,
            liberacao,
            recursoAutenticado,
            storage,
            historico,
            roteamento,
            eventos,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void avancaSomenteDepoisDeRegistrarEntregaDaEtapa() {
        UUID etapaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        UUID proximaEtapaId = UUID.randomUUID();
        UsuarioAutenticado ator = new UsuarioAutenticado("ator@teste.invalid", false);
        Recurso recurso = mock(Recurso.class);
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        EtapaOrdemServico proxima = mock(EtapaOrdemServico.class);
        OrdemServico ordemServico = mock(OrdemServico.class);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "entrega.docx", "application/octet-stream", new byte[]{1});

        when(recursoAutenticado.resolver(ator)).thenReturn(recurso);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(recurso)).thenReturn(true);
        when(etapa.getOrdemServico()).thenReturn(ordemServico);
        when(ordemServico.getId()).thenReturn(osId);
        when(ordemServico.getStatus()).thenReturn(StatusOrdemServico.EM_EXECUCAO);
        when(recurso.getEmail()).thenReturn("recurso@teste.invalid");
        when(proxima.getId()).thenReturn(proximaEtapaId);
        when(proxima.getStatus()).thenReturn(StatusEtapaOrdemServico.DISPARADA);
        when(roteamento.rotear(etapa)).thenReturn(
                new RoteamentoEtapaService.Resultado(Optional.of(proxima), true, 3));
        when(etapa.getNome()).thenReturn("Tradução");
        when(etapa.getId()).thenReturn(etapaId);
        when(etapa.getEntregueEm()).thenReturn(AGORA);
        when(etapa.getArquivoEntregueNome()).thenReturn("entrega.docx");
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ACEITA,
                StatusEtapaOrdemServico.ENTREGUE);
        when(liberacao.estaLiberada(etapa)).thenReturn(true);
        when(storage.salvarTodos(anyList())).thenReturn(List.of(new ArquivoArmazenado(
                "entrega.docx", "2026/11/entrega.docx", "application/octet-stream", 1)));
        when(etapas.entregar(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        EntregaEtapaResponse resposta = service.entregar(etapaId, arquivo, ator);

        var ordem = inOrder(etapas, roteamento);
        ordem.verify(etapas).entregar(eq(etapaId), eq(recurso), eq("entrega.docx"),
                eq("2026/11/entrega.docx"), eq(AGORA), eq(StatusEtapaOrdemServico.ACEITA),
                eq(StatusEtapaOrdemServico.ENTREGUE));
        ordem.verify(roteamento).rotear(etapa);
        verify(etapas, never()).save(any());
        ArgumentCaptor<HistoricoEtapa> historicos = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico, times(2)).save(historicos.capture());
        assertEquals(EventoEtapa.ENTREGA, historicos.getAllValues().getFirst().getEvento());
        assertEquals("recurso@teste.invalid", historicos.getAllValues().getFirst().getRealizadoPor());
        assertEquals(EventoEtapa.AVANCO, historicos.getAllValues().getLast().getEvento());
        assertEquals("sistema", historicos.getAllValues().getLast().getRealizadoPor());
        assertEquals(StatusEtapaOrdemServico.ENTREGUE, etapa.getStatus());
        assertEquals("entrega.docx", resposta.arquivoNome());
        assertEquals("/api/v1/recurso/etapas/" + etapaId + "/entrega/arquivo", resposta.arquivoUrl());
        assertEquals(proximaEtapaId, resposta.avanco().proximaEtapaId());
        assertEquals(StatusEtapaOrdemServico.DISPARADA, resposta.avanco().proximaEtapaStatus());
        assertTrue(resposta.avanco().conviteAutomatico());
        assertEquals(3, resposta.avanco().recursosNotificados());
        assertEquals(osId, resposta.ordemServico().id());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, resposta.ordemServico().status());
    }

    @Test
    void ultimaEntregaRetornaAvancoNuloEOrdemEmVerificacao() {
        UUID etapaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        UsuarioAutenticado ator = new UsuarioAutenticado("recurso@teste.invalid", false);
        Recurso recurso = mock(Recurso.class);
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        OrdemServico ordemServico = mock(OrdemServico.class);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "entrega.docx", "application/octet-stream", new byte[]{1});

        when(recursoAutenticado.resolver(ator)).thenReturn(recurso);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(recurso)).thenReturn(true);
        when(etapa.getOrdemServico()).thenReturn(ordemServico);
        when(ordemServico.getId()).thenReturn(osId);
        when(ordemServico.getStatus()).thenReturn(StatusOrdemServico.AGUARDANDO_VERIFICACAO);
        when(recurso.getEmail()).thenReturn(ator.email());
        when(etapa.getId()).thenReturn(etapaId);
        when(etapa.getNome()).thenReturn("Entrega");
        when(etapa.getEntregueEm()).thenReturn(AGORA);
        when(etapa.getArquivoEntregueNome()).thenReturn("entrega.docx");
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ACEITA,
                StatusEtapaOrdemServico.ENTREGUE);
        when(liberacao.estaLiberada(etapa)).thenReturn(true);
        when(storage.salvarTodos(anyList())).thenReturn(List.of(new ArquivoArmazenado(
                "entrega.docx", "2026/11/entrega.docx", "application/octet-stream", 1)));
        when(etapas.entregar(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(roteamento.rotear(etapa)).thenReturn(
                new RoteamentoEtapaService.Resultado(Optional.empty(), false, 0));

        EntregaEtapaResponse resposta = service.entregar(etapaId, arquivo, ator);

        assertNull(resposta.avanco());
        assertEquals(osId, resposta.ordemServico().id());
        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, resposta.ordemServico().status());
    }
}
