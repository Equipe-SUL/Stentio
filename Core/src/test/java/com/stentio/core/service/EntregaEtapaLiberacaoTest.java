package com.stentio.core.service;

import com.stentio.core.exception.TransicaoEtapaInvalidaException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntregaEtapaLiberacaoTest {

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
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void bloqueiaEntregaAceitaAntesDaLiberacaoSemGravarArquivo() {
        UUID etapaId = UUID.randomUUID();
        UsuarioAutenticado ator = new UsuarioAutenticado("recurso@teste.invalid", false);
        Recurso recurso = mock(Recurso.class);
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "entrega.docx", "application/octet-stream", new byte[]{1});

        when(recursoAutenticado.resolver(ator)).thenReturn(recurso);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(recurso)).thenReturn(true);
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA);
        when(etapa.getNome()).thenReturn("Revisão");
        when(liberacao.estaLiberada(etapa)).thenReturn(false);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> service.entregar(etapaId, arquivo, ator));

        verify(storage, never()).salvarTodos(anyList());
        verify(etapas, never()).save(any());
        verify(historico, never()).save(any());
    }
}
