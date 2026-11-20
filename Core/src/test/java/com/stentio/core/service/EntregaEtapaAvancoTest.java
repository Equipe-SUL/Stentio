package com.stentio.core.service;

import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntregaEtapaAvancoTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EtapaLiberacaoService liberacao = mock(EtapaLiberacaoService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final AvancoEtapaService avanco = mock(AvancoEtapaService.class);
    private final EntregaEtapaService service = new EntregaEtapaService(
            etapas,
            liberacao,
            recursoAutenticado,
            storage,
            historico,
            avanco,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void avancaSomenteDepoisDeRegistrarEntregaDaEtapa() {
        UUID etapaId = UUID.randomUUID();
        UsuarioAutenticado ator = new UsuarioAutenticado("recurso@teste.invalid", false);
        Recurso recurso = mock(Recurso.class);
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "entrega.docx", "application/octet-stream", new byte[]{1});

        when(recursoAutenticado.resolver(ator)).thenReturn(recurso);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(recurso)).thenReturn(true);
        when(etapa.getNome()).thenReturn("Tradução");
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ENTREGUE,
                StatusEtapaOrdemServico.ENTREGUE);
        when(liberacao.estaLiberada(etapa)).thenReturn(true);
        when(storage.salvarTodos(anyList())).thenReturn(List.of(new ArquivoArmazenado(
                "entrega.docx", "2026/11/entrega.docx", "application/octet-stream", 1)));

        service.entregar(etapaId, arquivo, ator);

        var ordem = inOrder(etapa, avanco);
        ordem.verify(etapa).entregar("entrega.docx", "2026/11/entrega.docx", AGORA, true);
        ordem.verify(avanco).avancar(etapa);
        verify(etapas).save(etapa);
        assertEquals(StatusEtapaOrdemServico.ENTREGUE, etapa.getStatus());
    }
}
