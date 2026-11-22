package com.stentio.core.service;

import org.springframework.context.ApplicationEventPublisher;
import com.stentio.core.support.AtorAuditoria;
import com.stentio.core.event.StatusAlteradoEvento;
import com.stentio.core.model.EntregaFinalEmailOutbox;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.EntregaFinalEmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-015 CA.8 e CA.9: é o ACK que encerra a OS, e só ele.
 *
 * <p>Este é o teste que justifica o desenho: sem ele, nada impede alguém de mover o
 * encerramento para a requisição do gestor e abrir a janela de OS fechada sem e-mail.
 */
class EntregaFinalEmailDispatcherTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");

    private final EntregaFinalEmailOutboxRepository repository =
            mock(EntregaFinalEmailOutboxRepository.class);
    private final EntregaFinalEmailPublisher publisher = mock(EntregaFinalEmailPublisher.class);

    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EntregaFinalEmailDispatcher dispatcher = new EntregaFinalEmailDispatcher(
            repository, publisher, eventos, Clock.fixed(AGORA, ZoneOffset.UTC), 30);

    private OrdemServico ordem;
    private EntregaFinalEmailOutbox envio;

    @BeforeEach
    void preparar() {
        Orcamento orcamento = mock(Orcamento.class);
        when(orcamento.getSolicitacao()).thenReturn(mock(Solicitacao.class));

        ordem = new OrdemServico(orcamento);
        ReflectionTestUtils.setField(ordem, "id", UUID.randomUUID());
        ordem.planejar();
        ordem.iniciarExecucao();
        ordem.aguardarVerificacao();

        envio = new EntregaFinalEmailOutbox(
                ordem, "ENTREGA_FINAL:x", "gestor@aliancatraducoes.com", "{}", AGORA);
        ReflectionTestUtils.setField(envio, "id", UUID.randomUUID());

        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.of(envio));
    }

    @Test
    @DisplayName("CA.8: com o ACK, o envio é marcado e a OS encerra")
    void ackEncerraAOrdem() {
        assertTrue(dispatcher.processarProximo());

        assertEquals(AGORA, envio.getEnviadoEm());
        assertEquals(AGORA, envio.getEncerradoEm());
        assertNull(envio.getPayload());
        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
        assertEquals(AGORA, ordem.getEntregueEm());
        assertEquals("gestor@aliancatraducoes.com", ordem.getEntreguePor());
    }

    @Test
    @DisplayName("US-019: o encerramento entra no log como Sistema, com o aprovador na observação")
    void encerramentoAuditadoComoSistema() {
        dispatcher.processarProximo();

        org.mockito.ArgumentCaptor<StatusAlteradoEvento> captor =
                org.mockito.ArgumentCaptor.forClass(StatusAlteradoEvento.class);
        org.mockito.Mockito.verify(eventos).publishEvent(captor.capture());
        StatusAlteradoEvento evento = captor.getValue();

        assertEquals("AGUARDANDO_VERIFICACAO", evento.statusAnterior());
        assertEquals("ENCERRADA", evento.statusNovo());
        assertEquals(AtorAuditoria.PERFIL_SISTEMA, evento.ator().perfil());
        assertTrue(evento.observacao().contains("gestor@aliancatraducoes.com"));
    }

    @Test
    @DisplayName("US-019: sem ACK não há encerramento, e nada vai para o log")
    void falhaNaoAudita() throws Exception {
        doThrow(new RuntimeException("broker fora")).when(publisher).publicar(any());

        dispatcher.processarProximo();

        org.mockito.Mockito.verify(eventos, org.mockito.Mockito.never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("CA.9: falha de publicação não encerra nada e agenda nova tentativa")
    void falhaMantemAOrdemAguardando() throws Exception {
        doThrow(new IllegalStateException("broker fora")).when(publisher).publicar(any());

        assertTrue(dispatcher.processarProximo());

        assertNull(envio.getEnviadoEm());
        assertEquals(1, envio.getTentativas());
        assertEquals(AGORA.plusSeconds(30), envio.getProximaTentativaEm());
        assertEquals(StatusOrdemServico.AGUARDANDO_VERIFICACAO, ordem.getStatus());
        assertNotNull(envio.getPayload());
    }

    @Test
    @DisplayName("publicado mas sem conseguir encerrar: o envio continua marcado, para não republicar")
    void encerramentoQueFalhaNaoDesfazOEnvio() {
        ordem.reprovarEntrega("reprovada por fora do fluxo");

        assertTrue(dispatcher.processarProximo());

        assertEquals(AGORA, envio.getEnviadoEm());
        assertNull(envio.getPayload());
        assertNull(envio.getEncerradoEm());
        assertEquals(AGORA.plusSeconds(30), envio.getProximaTentativaEm());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());
    }

    @Test
    @DisplayName("linha publicada sem encerramento só tenta fechar a OS, sem publicar de novo")
    void publicadaSemEncerramentoNaoRepublica() throws Exception {
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.empty());
        envio.setEnviadoEm(AGORA);
        when(repository.buscarPublicadaSemEncerramentoComLock(AGORA))
                .thenReturn(Optional.of(envio));

        assertTrue(dispatcher.processarProximo());

        verify(publisher, never()).publicar(any());
        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
        assertEquals(AGORA, envio.getEncerradoEm());
    }

    @Test
    @DisplayName("sem pendência, o dispatcher não faz nada")
    void semPendenciaNaoProcessa() {
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.empty());

        assertFalse(dispatcher.processarProximo());
    }
}
