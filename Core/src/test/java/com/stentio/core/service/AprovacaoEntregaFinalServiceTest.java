package com.stentio.core.service;

import com.stentio.core.dto.AprovacaoEntregaFinalResponse;
import com.stentio.core.exception.AnexoAcimaDoLimiteException;
import com.stentio.core.exception.ArquivoEntregueIndisponivelException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.EntregaFinalEmailOutbox;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-015 CA.5 a CA.9: o que a aprovação grava, e o que ela recusa antes de gravar. */
class AprovacaoEntregaFinalServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant ENTREGUE_EM = Instant.parse("2026-11-20T18:30:00Z");
    private static final long LIMITE = 1_000;

    private final EntregaFinalService revisao = mock(EntregaFinalService.class);
    private final EntregaFinalEmailOutboxRepository outbox = mock(EntregaFinalEmailOutboxRepository.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);

    private final AprovacaoEntregaFinalService service = new AprovacaoEntregaFinalService(
            revisao, outbox, historico, storage, new ObjectMapper(),
            Clock.fixed(AGORA, ZoneOffset.UTC), LIMITE);

    private final UUID osId = UUID.randomUUID();
    private final UsuarioAutenticado gestor =
            new UsuarioAutenticado("gestor@aliancatraducoes.com", UUID.randomUUID(), false);

    private OrdemServico ordem;
    private EtapaOrdemServico ultima;

    @BeforeEach
    void preparar() {
        ordem = ordemAguardandoVerificacao();
        ultima = etapaEntregue("Revisão", 2, "contrato-final.docx");

        when(revisao.buscarParaRevisao(osId)).thenReturn(ordem);
        when(revisao.entreguesEmOrdem(osId)).thenReturn(List.of(ultima));
        when(outbox.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(outbox.save(any())).thenAnswer(inv -> inv.getArgument(0));
        arquivoCom(500);
    }

    private OrdemServico ordemAguardandoVerificacao() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getEmailRepresentante()).thenReturn("representante@cliente.com");
        when(cliente.getNomeRepresentante()).thenReturn("Maria Souza");
        when(cliente.getNomeEmpresa()).thenReturn("Empresa XPTO");

        Orcamento orcamento = mock(Orcamento.class);
        when(orcamento.getSolicitacao()).thenReturn(mock(Solicitacao.class));
        when(orcamento.getCliente()).thenReturn(cliente);

        OrdemServico os = new OrdemServico(orcamento);
        ReflectionTestUtils.setField(os, "id", osId);
        ReflectionTestUtils.setField(os, "criadoEm", Instant.parse("2026-11-01T09:00:00Z"));
        os.planejar();
        os.iniciarExecucao();
        os.aguardarVerificacao();
        return os;
    }

    private EtapaOrdemServico etapaEntregue(String nome, int ordemDaEtapa, String arquivo) {
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome(nome);

        EtapaOrdemServico etapa = new EtapaOrdemServico(ordem, ordemDaEtapa, nome, tipo, 8);
        ReflectionTestUtils.setField(etapa, "id", UUID.randomUUID());

        Recurso executante = mock(Recurso.class);
        when(executante.getId()).thenReturn(UUID.randomUUID());
        when(executante.elegivelParaAlocacao()).thenReturn(true);

        etapa.alocar(executante, Instant.parse("2026-11-25T12:00:00Z"), ENTREGUE_EM);
        etapa.aceitarPor(executante, ENTREGUE_EM, true);
        etapa.entregar(arquivo, "2026/11/" + arquivo, ENTREGUE_EM, true);
        return etapa;
    }

    private void arquivoCom(int bytes) {
        when(storage.carregar(anyString())).thenReturn(new ByteArrayResource(new byte[bytes]));
    }

    @Test
    @DisplayName("Cenário 1: aprovar grava a outbox e mantém a OS aguardando verificação")
    void aprovarGravaAOutbox() {
        AprovacaoEntregaFinalResponse resposta = service.aprovar(osId, "Segue o documento final.", gestor);

        ArgumentCaptor<EntregaFinalEmailOutbox> captor =
                ArgumentCaptor.forClass(EntregaFinalEmailOutbox.class);
        verify(outbox).save(captor.capture());

        EntregaFinalEmailOutbox gravado = captor.getValue();
        assertEquals("gestor@aliancatraducoes.com", gravado.getAprovadoPor());
        assertTrue(gravado.getPayload().contains("representante@cliente.com"));
        assertTrue(gravado.getPayload().contains("contrato-final.docx"));
        // RN.7: a OS só fecha com o ACK; aqui ela ainda espera.
        assertEquals(com.stentio.core.model.StatusOrdemServico.AGUARDANDO_VERIFICACAO, resposta.status());
    }

    @Test
    @DisplayName("RN.6: a chave de idempotência leva o entregue_em da última etapa")
    void chaveUsaOEntregueEmDaUltimaEtapa() {
        AprovacaoEntregaFinalResponse resposta = service.aprovar(osId, null, gestor);

        assertEquals("ENTREGA_FINAL:" + osId + ":" + ENTREGUE_EM, resposta.idempotencyKey());
    }

    @Test
    @DisplayName("Cenário 6: aprovar de novo devolve a mesma mensagem sem criar outra")
    void duplaAprovacaoNaoCriaSegundaMensagem() {
        EntregaFinalEmailOutbox existente = new EntregaFinalEmailOutbox(
                ordem, "ENTREGA_FINAL:" + osId + ":" + ENTREGUE_EM,
                "gestor@aliancatraducoes.com", "{}", AGORA);
        ReflectionTestUtils.setField(existente, "id", UUID.randomUUID());
        when(outbox.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existente));

        AprovacaoEntregaFinalResponse resposta = service.aprovar(osId, null, gestor);

        assertEquals(existente.getIdempotencyKey(), resposta.idempotencyKey());
        verify(outbox, never()).save(any());
        verify(historico, never()).save(any());
    }

    @Test
    @DisplayName("Cenário 3: arquivo acima do limite não grava nada")
    void arquivoAcimaDoLimiteNaoGravaNada() {
        arquivoCom((int) LIMITE + 1);

        assertThrows(AnexoAcimaDoLimiteException.class, () -> service.aprovar(osId, null, gestor));

        verify(outbox, never()).save(any());
        verify(historico, never()).save(any());
    }

    @Test
    @DisplayName("CA.6: arquivo que não pode ser lido vira 422, não um envio quebrado")
    void arquivoIlegivelNaoGravaNada() {
        when(storage.carregar(anyString())).thenThrow(new RuntimeException("sumiu do volume"));

        assertThrows(ArquivoEntregueIndisponivelException.class, () -> service.aprovar(osId, null, gestor));

        verify(outbox, never()).save(any());
    }

    @Test
    @DisplayName("sem etapa entregue não há o que aprovar")
    void semEtapaEntregue() {
        when(revisao.entreguesEmOrdem(osId)).thenReturn(List.of());

        assertThrows(ArquivoEntregueIndisponivelException.class, () -> service.aprovar(osId, null, gestor));
    }

    @Test
    @DisplayName("CA.7: o histórico da aprovação entra junto com a outbox")
    void gravaHistoricoDaAprovacao() {
        service.aprovar(osId, null, gestor);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico).save(captor.capture());
        assertEquals(EventoEtapa.APROVACAO_FINAL, captor.getValue().getEvento());
        assertEquals("gestor@aliancatraducoes.com", captor.getValue().getRealizadoPor());
    }
}
