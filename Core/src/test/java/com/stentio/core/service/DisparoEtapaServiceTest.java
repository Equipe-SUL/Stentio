package com.stentio.core.service;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-013 CA.1, CA.2 e RN.6: abrir a etapa para todos os elegíveis. */
class DisparoEtapaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-24T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final ConviteEtapaEmailService convites = mock(ConviteEtapaEmailService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);

    private final DisparoEtapaService service = new DisparoEtapaService(
            etapas, recursos, convites, historico, Clock.fixed(AGORA, ZoneOffset.UTC));

    private final UUID osId = UUID.randomUUID();
    private final UUID etapaId = UUID.randomUUID();
    private final UsuarioAutenticado gestor = new UsuarioAutenticado("gestor@stentio.com", false);

    private UUID portuguesId;
    private UUID inglesId;
    private TipoServico traducao;
    private EtapaOrdemServico etapa;

    @BeforeEach
    void preparar() {
        Idioma portugues = idioma();
        Idioma ingles = idioma();
        portuguesId = portugues.getId();
        inglesId = ingles.getId();

        traducao = new TipoServico();
        traducao.setId(UUID.randomUUID());
        traducao.setNome("Tradução");

        Solicitacao solicitacao = mock(Solicitacao.class);
        when(solicitacao.getIdiomaOrigem()).thenReturn(portugues);
        when(solicitacao.getIdiomaDestino()).thenReturn(ingles);

        OrdemServico ordem = mock(OrdemServico.class);
        when(ordem.getSolicitacao()).thenReturn(solicitacao);

        etapa = new EtapaOrdemServico(ordem, 1, "Tradução Principal", traducao, 24);

        when(etapas.buscarParaAlocacao(osId, etapaId)).thenReturn(Optional.of(etapa));
        when(etapas.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Idioma idioma() {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        return i;
    }

    private void elegiveis(Recurso... pessoas) {
        when(recursos.buscarDisponiveis(portuguesId, inglesId, traducao.getId()))
                .thenReturn(List.of(pessoas));
    }

    private static Recurso qualquerRecurso() {
        return mock(Recurso.class);
    }

    @Test
    @DisplayName("CA.1: dispara, conta os notificados e abre a etapa")
    void disparaParaOsElegiveis() {
        elegiveis(qualquerRecurso(), qualquerRecurso(), qualquerRecurso());

        DisparoEtapaResponse resposta = service.disparar(osId, etapaId, gestor);

        assertEquals(StatusEtapaOrdemServico.DISPARADA, resposta.status());
        assertEquals(3, resposta.recursosNotificados());
        assertEquals(AGORA, etapa.getDisparadaEm());
    }

    @Test
    @DisplayName("CA.2: os convites saem na mesma transação do disparo")
    void convidaTodosOsElegiveis() {
        elegiveis(qualquerRecurso(), qualquerRecurso());

        service.disparar(osId, etapaId, gestor);

        verify(convites).convidar(same(etapa), anyList(), same(AGORA));
    }

    @Test
    @DisplayName("sem ninguém elegível o disparo falha em vez de deixar a etapa presa")
    void semElegiveisNaoDispara() {
        elegiveis();

        assertThrows(AlocacaoInvalidaException.class, () -> service.disparar(osId, etapaId, gestor));

        assertEquals(StatusEtapaOrdemServico.PENDENTE, etapa.getStatus());
        verify(convites, never()).convidar(any(), anyList(), any());
        verify(historico, never()).save(any());
    }

    @Test
    @DisplayName("RN.6: etapa já aceita não volta para a fila")
    void naoDisparaEtapaJaAceita() {
        Recurso ana = mock(Recurso.class);
        when(ana.elegivelParaAlocacao()).thenReturn(true);
        etapa.alocar(ana, PRAZO, AGORA);
        etapa.aceitarPor(ana, AGORA);
        elegiveis(qualquerRecurso());

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> service.disparar(osId, etapaId, gestor));

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa.getStatus());
        verify(convites, never()).convidar(any(), anyList(), any());
    }

    @Test
    @DisplayName("CA.8: o disparo fica no histórico sem recurso, porque não é dirigido a ninguém")
    void gravaHistoricoSemRecurso() {
        elegiveis(qualquerRecurso());

        service.disparar(osId, etapaId, gestor);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico).save(captor.capture());
        assertEquals(EventoEtapa.DISPARO, captor.getValue().getEvento());
        assertEquals("gestor@stentio.com", captor.getValue().getRealizadoPor());
        assertNull(captor.getValue().getRecursoNovo());
    }
}
