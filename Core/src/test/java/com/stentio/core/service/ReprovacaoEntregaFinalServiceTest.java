package com.stentio.core.service;

import com.stentio.core.dto.ReprovacaoEntregaFinalResponse;
import com.stentio.core.exception.TransicaoOrdemServicoInvalidaException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-015 CA.10 a CA.12: a reprovação devolve a etapa sem mexer no que o gestor não decidiu. */
class ReprovacaoEntregaFinalServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant ENTREGUE_EM = Instant.parse("2026-11-20T18:30:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-25T12:00:00Z");
    private static final String MOTIVO = "Falta a assinatura no último documento";

    private final EntregaFinalService revisao = mock(EntregaFinalService.class);
    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final EntregaFinalEmailOutboxRepository outbox = mock(EntregaFinalEmailOutboxRepository.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final ReaberturaEtapaEmailService avisos = mock(ReaberturaEtapaEmailService.class);

    private final ReprovacaoEntregaFinalService service = new ReprovacaoEntregaFinalService(
            revisao, etapas, outbox, historico, avisos, Clock.fixed(AGORA, ZoneOffset.UTC));

    private final UUID osId = UUID.randomUUID();
    private final UsuarioAutenticado gestor =
            new UsuarioAutenticado("gestor@aliancatraducoes.com", UUID.randomUUID(), false);

    private OrdemServico ordem;
    private EtapaOrdemServico ultima;
    private Recurso executante;

    @BeforeEach
    void preparar() {
        Cliente cliente = mock(Cliente.class);
        Orcamento orcamento = mock(Orcamento.class);
        when(orcamento.getSolicitacao()).thenReturn(mock(Solicitacao.class));
        when(orcamento.getCliente()).thenReturn(cliente);

        ordem = new OrdemServico(orcamento);
        ReflectionTestUtils.setField(ordem, "id", osId);
        ordem.planejar();
        ordem.iniciarExecucao();
        ordem.aguardarVerificacao();

        executante = mock(Recurso.class);
        when(executante.getId()).thenReturn(UUID.randomUUID());
        when(executante.elegivelParaAlocacao()).thenReturn(true);

        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Revisão");

        ultima = new EtapaOrdemServico(ordem, 2, "Revisão", tipo, 8);
        ReflectionTestUtils.setField(ultima, "id", UUID.randomUUID());
        ultima.alocar(executante, PRAZO, ENTREGUE_EM);
        ultima.aceitarPor(executante, ENTREGUE_EM, true);
        ultima.entregar("contrato-final.docx", "2026/11/contrato-final.docx", ENTREGUE_EM, true);

        when(revisao.buscarParaRevisao(osId)).thenReturn(ordem);
        when(revisao.entreguesEmOrdem(osId)).thenReturn(List.of(ultima));
        when(outbox.existsByOrdemServicoIdAndEnviadoEmIsNull(osId)).thenReturn(false);
    }

    @Test
    @DisplayName("Cenário 4: a etapa volta para ACEITA e a OS para EM_EXECUCAO")
    void reprovarReabreAUltimaEtapa() {
        ReprovacaoEntregaFinalResponse resposta = service.reprovar(osId, MOTIVO, gestor);

        assertEquals(StatusEtapaOrdemServico.ACEITA, resposta.etapaStatus());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, resposta.ordemServicoStatus());
        assertEquals(MOTIVO, ordem.getMotivoRevisao());
    }

    @Test
    @DisplayName("RN.9: a reabertura não mexe em recurso, alocação nem prazo")
    void reaberturaPreservaAlocacaoEPrazo() {
        service.reprovar(osId, MOTIVO, gestor);

        assertEquals(executante, ultima.getRecurso());
        assertEquals(PRAZO, ultima.getDataLimiteEntrega());
        assertEquals(ENTREGUE_EM, ultima.getAlocadoEm());
        // O arquivo reprovado continua lá: o gestor ainda precisa poder rever o que recusou.
        assertEquals("contrato-final.docx", ultima.getArquivoEntregueNome());
    }

    @Test
    @DisplayName("CA.10: grava REABERTURA com o e-mail do gestor e avisa o profissional")
    void registraEAvisa() {
        service.reprovar(osId, MOTIVO, gestor);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico).save(captor.capture());
        assertEquals(EventoEtapa.REABERTURA, captor.getValue().getEvento());
        assertEquals("gestor@aliancatraducoes.com", captor.getValue().getRealizadoPor());

        verify(avisos).avisarReabertura(ultima, MOTIVO, AGORA);
    }

    @Test
    @DisplayName("CA.12: com envio aguardando confirmação, reprovar é conflito")
    void naoReprovaComEnvioPendente() {
        when(outbox.existsByOrdemServicoIdAndEnviadoEmIsNull(osId)).thenReturn(true);

        assertThrows(TransicaoOrdemServicoInvalidaException.class,
                () -> service.reprovar(osId, MOTIVO, gestor));

        assertEquals(StatusEtapaOrdemServico.ENTREGUE, ultima.getStatus());
        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, ordem.getStatus());
        verify(avisos, never()).avisarReabertura(any(), any(), any());
    }

    @Test
    @DisplayName("Cenário 7: OS já encerrada não é reprovada")
    void naoReprovaOrdemEncerrada() {
        ordem.encerrar(AGORA, "gestor@aliancatraducoes.com");
        doThrowAoExigirVerificacao();

        assertThrows(TransicaoOrdemServicoInvalidaException.class,
                () -> service.reprovar(osId, MOTIVO, gestor));

        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
        verify(historico, never()).save(any());
    }

    /**
     * A guarda de estado mora no EntregaFinalService, que aqui é dublê. O teste do estado
     * em si está em OrdemServicoEntregaFinalTest; este garante que a reprovação respeita
     * a recusa em vez de seguir em frente.
     */
    private void doThrowAoExigirVerificacao() {
        org.mockito.Mockito.doThrow(new TransicaoOrdemServicoInvalidaException(
                        "A ordem de serviço não está aguardando verificação"))
                .when(revisao).exigirAguardandoVerificacao(ordem);
        assertNotNull(ordem);
    }
}
