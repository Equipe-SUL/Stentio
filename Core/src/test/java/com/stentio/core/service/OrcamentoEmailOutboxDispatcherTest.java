package com.stentio.core.service;

import com.stentio.core.model.OrcamentoEmailOutbox;
import com.stentio.core.model.Orcamento;
import com.stentio.core.repository.OrcamentoEmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrcamentoEmailOutboxDispatcherTest {
    private static final Instant AGORA = Instant.parse("2026-10-10T17:00:00Z");
    private final OrcamentoEmailOutboxRepository repository = mock(OrcamentoEmailOutboxRepository.class);
    private final OrcamentoEmailPublisher publisher = mock(OrcamentoEmailPublisher.class);
    private OrcamentoEmailOutboxDispatcher dispatcher;
    private OrcamentoEmailOutbox email;

    @BeforeEach
    void preparar() {
        dispatcher = new OrcamentoEmailOutboxDispatcher(repository, publisher,
                Clock.fixed(AGORA, ZoneOffset.UTC), 30);
        email = new OrcamentoEmailOutbox();
        email.setId(UUID.randomUUID());
        Orcamento orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        email.setOrcamento(orcamento);
        email.setPayload("{\"to\":\"cliente@example.com\"}");
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.of(email));
    }

    @Test
    void confirmaPublicacaoEApagaPayloadComToken() throws Exception {
        assertTrue(dispatcher.processarProximo());
        verify(publisher).publicar(email);
        assertEquals(AGORA, email.getEnviadoEm());
        assertNull(email.getPayload());
        assertEquals(1, email.getTentativas());
    }

    @Test
    void falhaAgendaNovaTentativaSemMarcarComoEnviado() throws Exception {
        doThrow(new IllegalStateException("broker indisponível")).when(publisher).publicar(email);

        assertTrue(dispatcher.processarProximo());

        assertNull(email.getEnviadoEm());
        assertEquals("{\"to\":\"cliente@example.com\"}", email.getPayload());
        assertEquals(AGORA.plusSeconds(30), email.getProximaTentativaEm());
    }

    @Test
    void filaVaziaNaoPublica() {
        when(repository.buscarPendenteComLock(AGORA)).thenReturn(Optional.empty());

        assertFalse(dispatcher.processarProximo());

        verifyNoInteractions(publisher);
    }
}
