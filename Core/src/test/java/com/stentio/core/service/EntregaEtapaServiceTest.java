package com.stentio.core.service;

import org.springframework.context.ApplicationEventPublisher;
import com.stentio.core.dto.EntregaEtapaResponse;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntregaEtapaServiceTest {

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EtapaLiberacaoService liberacao = mock(EtapaLiberacaoService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final RoteamentoEtapaService roteamento = mock(RoteamentoEtapaService.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EntregaEtapaService service = new EntregaEtapaService(
            etapas, liberacao, recursoAutenticado, storage, historico, roteamento,
            eventos,
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC));

    private final UUID etapaId = UUID.randomUUID();
    private final UsuarioAutenticado ator = new UsuarioAutenticado("ana@exemplo.com", UUID.randomUUID(), false);
    private final Recurso ana = mock(Recurso.class);
    private final EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);

    @BeforeEach
    void preparar() {
        when(liberacao.estaLiberada(etapa)).thenReturn(true);
        when(recursoAutenticado.resolver(ator)).thenReturn(ana);
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(etapa.pertenceA(ana)).thenReturn(true);
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ENTREGUE);
        when(etapa.getArquivoEntregueNome()).thenReturn("traducao.docx");
        when(etapa.getArquivoEntregueCaminho()).thenReturn("2026/11/id.docx");
    }

    @Test
    void recursoLocalizaSuaEntrega() {
        EntregaEtapaService.ArquivoEntregue arquivo = service.localizarEntregaDoRecurso(etapaId, ator);

        assertEquals("traducao.docx", arquivo.nomeOriginal());
        assertEquals("2026/11/id.docx", arquivo.caminho());
    }

    @Test
    void recursoNaoLocalizaEntregaDeOutraPessoa() {
        when(etapa.pertenceA(ana)).thenReturn(false);

        assertThrows(EtapaNaoEncontradaException.class,
                () -> service.localizarEntregaDoRecurso(etapaId, ator));
    }

    @Test
    void entregaAindaNaoFinalizadaNaoPodeSerBaixada() {
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA);
        when(etapa.getArquivoEntregueCaminho()).thenReturn(null);

        assertThrows(SolicitacaoInvalidaException.class,
                () -> service.localizarEntregaDoRecurso(etapaId, ator));
    }

    @Test
    void entregaAtualizaSomenteSeAEtapaContinuarAceita() {
        MultipartFile arquivo = arquivoValido();
        prepararEntrega();
        when(etapas.entregar(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(roteamento.rotear(etapa))
                .thenReturn(new RoteamentoEtapaService.Resultado(Optional.empty(), false, 0));
        when(etapa.getId()).thenReturn(etapaId);
        com.stentio.core.model.OrdemServico ordem = mock(com.stentio.core.model.OrdemServico.class);
        when(etapa.getOrdemServico()).thenReturn(ordem);
        when(ordem.getId()).thenReturn(UUID.randomUUID());
        when(ordem.getStatus()).thenReturn(StatusOrdemServico.AGUARDANDO_VERIFICACAO);

        EntregaEtapaResponse resposta = service.entregar(etapaId, arquivo, ator);

        verify(etapas).entregar(any(), any(), any(), any(), any(), any(), any());
        verify(etapas, never()).save(any());
        assertNull(resposta.avanco());
        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, resposta.ordemServico().status());
    }

    @Test
    void entregaConcorrenteNaoGravaNemDeixaArquivoOrfao() {
        MultipartFile arquivo = arquivoValido();
        prepararEntrega();
        when(etapas.entregar(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> service.entregar(etapaId, arquivo, ator));

        verify(etapas, never()).save(any());
        verify(storage).remover(List.of("2026/11/novo.docx"));
    }

    private MultipartFile arquivoValido() {
        MultipartFile arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(storage.salvarTodos(anyList()))
                .thenReturn(List.of(new ArquivoArmazenado("novo.docx", "2026/11/novo.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 10)));
        return arquivo;
    }

    private void prepararEntrega() {
        when(etapa.getStatus()).thenReturn(StatusEtapaOrdemServico.ACEITA);
    }
}
