package com.stentio.core.service;

import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntregaEtapaServiceTest {

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EtapaLiberacaoService liberacao = mock(EtapaLiberacaoService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final RoteamentoEtapaService roteamento = mock(RoteamentoEtapaService.class);
    private final EntregaEtapaService service = new EntregaEtapaService(
            etapas, liberacao, recursoAutenticado, storage, historico, roteamento,
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
}
