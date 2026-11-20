package com.stentio.core.service;

import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.NotificacaoEtapaOutbox;
import com.stentio.core.repository.NotificacaoEtapaOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoEtapaOutboxDispatcherTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");

    private final NotificacaoEtapaOutboxRepository repository =
            mock(NotificacaoEtapaOutboxRepository.class);
    private final NotificacaoEtapaPublisher publisher = mock(NotificacaoEtapaPublisher.class);
    private final NotificacaoEtapaOutboxDispatcher dispatcher = new NotificacaoEtapaOutboxDispatcher(
            repository, publisher, Clock.fixed(AGORA, ZoneOffset.UTC), 30);

    @Test
    @DisplayName("RabbitMQ indisponível mantém a notificação pendente e agenda nova tentativa")
    void falhaDePublicacaoMantemOutboxPendente() throws Exception {
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        when(etapa.getId()).thenReturn(UUID.randomUUID());
        NotificacaoEtapaOutbox notificacao =
                new NotificacaoEtapaOutbox(etapa, "ana@exemplo.com", "payload", AGORA);
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.of(notificacao));
        doThrow(new IllegalStateException("RabbitMQ indisponível")).when(publisher).publicar(notificacao);

        assertTrue(dispatcher.processarProximo());

        assertNull(notificacao.getEnviadoEm());
        assertEquals(1, notificacao.getTentativas());
        assertEquals(AGORA.plusSeconds(30), notificacao.getProximaTentativaEm());
        assertEquals("payload", notificacao.getPayload());
    }

    @Test
    @DisplayName("notificação confirmada não é publicada de novo")
    void confirmacaoNaoGeraReenvio() throws Exception {
        EtapaOrdemServico etapa = mock(EtapaOrdemServico.class);
        NotificacaoEtapaOutbox notificacao =
                new NotificacaoEtapaOutbox(etapa, "ana@exemplo.com", "payload", AGORA);
        when(repository.buscarPendenteComLock(AGORA))
                .thenReturn(Optional.of(notificacao))
                .thenReturn(Optional.empty());

        assertTrue(dispatcher.processarProximo());
        assertEquals(AGORA, notificacao.getEnviadoEm());
        assertNull(notificacao.getPayload());

        assertFalse(dispatcher.processarProximo());
        verify(publisher, times(1)).publicar(notificacao);
    }
}
