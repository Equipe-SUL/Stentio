package com.stentio.core.service;

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

    private final EntregaFinalEmailDispatcher dispatcher = new EntregaFinalEmailDispatcher(
            repository, publisher, Clock.fixed(AGORA, ZoneOffset.UTC), 30);

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
        assertNull(envio.getPayload());
        assertEquals(StatusOrdemServico.ENCERRADA, ordem.getStatus());
        assertEquals(AGORA, ordem.getEntregueEm());
        assertEquals("gestor@aliancatraducoes.com", ordem.getEntreguePor());
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
        // A OS saiu de AGUARDANDO_VERIFICACAO por fora. Depois do ACK a mensagem já está
        // no broker: desfazer tudo faria a próxima rodada mandar o pacote de novo.
        ordem.reprovarEntrega("reprovada por fora do fluxo");

        assertTrue(dispatcher.processarProximo());

        assertEquals(AGORA, envio.getEnviadoEm());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());
    }

    @Test
    @DisplayName("sem pendência, o dispatcher não faz nada")
    void semPendenciaNaoProcessa() {
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.empty());

        assertFalse(dispatcher.processarProximo());
    }
}
