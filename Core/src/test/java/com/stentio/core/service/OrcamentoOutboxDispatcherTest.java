package com.stentio.core.service;

import com.stentio.core.model.OrcamentoDecisaoOutbox;
import com.stentio.core.repository.OrcamentoDecisaoOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoOutboxDispatcherTest {
    @Mock OrcamentoDecisaoOutboxRepository repository;
    @Mock OrcamentoEventoPublisher publisher;
    private final Instant agora = Instant.parse("2026-10-10T17:00:00Z");
    private OrcamentoOutboxDispatcher dispatcher;
    private OrcamentoDecisaoOutbox evento;

    @BeforeEach
    void preparar() {
        dispatcher = new OrcamentoOutboxDispatcher(repository, publisher, Clock.fixed(agora, ZoneOffset.UTC), 30);
        evento = new OrcamentoDecisaoOutbox();
        evento.setId(UUID.randomUUID());
        evento.setProximaTentativaEm(agora);
        when(repository.buscarPendenteComLock(agora)).thenReturn(Optional.of(evento));
    }

    @Test
    void confirmaPublicacao() throws Exception {
        assertTrue(dispatcher.processarProximo());
        verify(publisher).publicar(evento);
        assertEquals(agora, evento.getPublicadoEm());
        assertEquals(1, evento.getTentativas());
    }

    @Test
    void falhaMantemEventoPendenteEAgendaRetry() throws Exception {
        doThrow(new IllegalStateException("broker indisponível")).when(publisher).publicar(evento);
        assertTrue(dispatcher.processarProximo());
        assertNull(evento.getPublicadoEm());
        assertEquals(agora.plusSeconds(30), evento.getProximaTentativaEm());
        assertEquals(1, evento.getTentativas());
    }

    @Test
    void nenhumaMensagemNaoPublica() {
        when(repository.buscarPendenteComLock(agora)).thenReturn(Optional.empty());
        assertFalse(dispatcher.processarProximo());
        verifyNoInteractions(publisher);
    }
}
