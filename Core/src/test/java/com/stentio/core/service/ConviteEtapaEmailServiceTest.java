package com.stentio.core.service;

import com.stentio.core.dto.NotificacaoEtapaMensagem;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.NotificacaoEtapaOutbox;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.NotificacaoEtapaOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConviteEtapaEmailServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");

    private final NotificacaoEtapaOutboxRepository outbox = mock(NotificacaoEtapaOutboxRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ConviteEtapaEmailService service =
            new ConviteEtapaEmailService(outbox, objectMapper, "https://portal.exemplo/recurso/demandas");

    private EtapaOrdemServico etapa;
    private Recurso recurso;

    @BeforeEach
    void preparar() throws Exception {
        UUID osId = UUID.randomUUID();
        OrdemServico ordem = mock(OrdemServico.class);
        Solicitacao solicitacao = mock(Solicitacao.class);
        Idioma origem = idioma("Português");
        Idioma destino = idioma("Inglês");
        TipoServico tipo = new TipoServico();
        tipo.setId(UUID.randomUUID());
        tipo.setNome("Tradução");
        recurso = mock(Recurso.class);
        etapa = mock(EtapaOrdemServico.class);

        when(ordem.getId()).thenReturn(osId);
        when(ordem.getCriadoEm()).thenReturn(AGORA);
        when(ordem.getSolicitacao()).thenReturn(solicitacao);
        when(solicitacao.getIdiomaOrigem()).thenReturn(origem);
        when(solicitacao.getIdiomaDestino()).thenReturn(destino);
        when(etapa.getOrdemServico()).thenReturn(ordem);
        when(etapa.getNome()).thenReturn("Tradução principal");
        when(etapa.getTipoServico()).thenReturn(tipo);
        when(etapa.getPrazoEstimadoHoras()).thenReturn(24);
        when(etapa.getDataLimiteEntrega()).thenReturn(null);
        when(recurso.getNome()).thenReturn("Ana");
        when(recurso.getEmail()).thenReturn("ana@exemplo.com");
        when(objectMapper.writeValueAsString(any(NotificacaoEtapaMensagem.class))).thenReturn("payload");
    }

    @Test
    void conviteAutomaticoExplicaQueAEtapaFoiLiberada() throws Exception {
        service.convidar(etapa, List.of(recurso), AGORA);

        ArgumentCaptor<NotificacaoEtapaMensagem> mensagem =
                ArgumentCaptor.forClass(NotificacaoEtapaMensagem.class);
        verify(objectMapper).writeValueAsString(mensagem.capture());

        assertEquals("ana@exemplo.com", mensagem.getValue().to());
        assertTrue(mensagem.getValue().subject().contains("Tradução principal"));
        assertTrue(mensagem.getValue().body().contains("liberada automaticamente"));
        assertTrue(mensagem.getValue().body().contains("https://portal.exemplo/recurso/demandas"));
        verificarUmaNotificacao();
    }

    @Test
    void avisoDeLiberacaoVaiSomenteAoRecursoDaEtapa() throws Exception {
        service.avisarEtapaLiberada(etapa, recurso, AGORA);

        ArgumentCaptor<NotificacaoEtapaMensagem> mensagem =
                ArgumentCaptor.forClass(NotificacaoEtapaMensagem.class);
        verify(objectMapper).writeValueAsString(mensagem.capture());

        assertEquals("ana@exemplo.com", mensagem.getValue().to());
        assertTrue(mensagem.getValue().subject().contains("Etapa liberada"));
        assertTrue(mensagem.getValue().body().contains("já pode começar"));
        verificarUmaNotificacao();
    }

    private void verificarUmaNotificacao() {
        AtomicReference<List<NotificacaoEtapaOutbox>> notificacoes = new AtomicReference<>();
        verify(outbox).saveAll(argThat(rows -> {
            notificacoes.set(StreamSupport.stream(rows.spliterator(), false).toList());
            return true;
        }));

        List<NotificacaoEtapaOutbox> gravadas = notificacoes.get();
        assertEquals(1, gravadas.size());
        assertSame(etapa, gravadas.getFirst().getEtapa());
        assertEquals("ana@exemplo.com", gravadas.getFirst().getDestinatario());
        assertEquals(AGORA, gravadas.getFirst().getCriadoEm());
    }

    private static Idioma idioma(String nome) {
        Idioma idioma = mock(Idioma.class);
        when(idioma.getId()).thenReturn(UUID.randomUUID());
        when(idioma.getNome()).thenReturn(nome);
        return idioma;
    }
}
