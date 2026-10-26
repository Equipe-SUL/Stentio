package com.stentio.core.service;

import com.stentio.core.model.*;
import com.stentio.core.repository.OrcamentoRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrcamentoTokenServiceTest {
    @Test
    void emitirNovoTokenInvalidaAnteriorESalvaApenasHash() {
        OrcamentoRepository repository = mock(OrcamentoRepository.class);
        Instant agora = Instant.parse("2026-10-10T17:00:00Z");
        Orcamento orcamento = new Orcamento();
        orcamento.setId(UUID.randomUUID());
        orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
        String anterior = TokenOrcamento.gerar().toString();
        orcamento.setTokenHash(TokenOrcamento.hash(anterior));
        orcamento.setTokenUtilizadoEm(agora.minusSeconds(10));
        when(repository.buscarPorIdComLock(orcamento.getId())).thenReturn(Optional.of(orcamento));
        var service = new OrcamentoTokenService(repository, Clock.fixed(agora, ZoneOffset.UTC));
        UUID token = service.emitir(orcamento.getId(), agora.plusSeconds(3600));
        assertEquals(4, token.version());
        assertEquals(TokenOrcamento.hash(token.toString()), orcamento.getTokenHash());
        assertNotEquals(TokenOrcamento.hash(anterior), orcamento.getTokenHash());
        assertNull(orcamento.getTokenUtilizadoEm());
        verify(repository).save(orcamento);
    }
}
