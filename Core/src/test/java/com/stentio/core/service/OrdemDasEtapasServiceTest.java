package com.stentio.core.service;

import com.stentio.core.exception.TransicaoEtapaInvalidaException;
import com.stentio.core.model.EtapaOrdemServico;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-014 T0 (CA.1 / RN.1): os services de disparo, aceite, entrega e recusa respeitam a
 * ordem das etapas. A regra mora na entidade; aqui o que se verifica é que cada service
 * busca a etapa anterior e barra antes de qualquer efeito colateral (convite, arquivo,
 * histórico).
 */
class OrdemDasEtapasServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-27T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);
    private final ConviteEtapaEmailService convites = mock(ConviteEtapaEmailService.class);
    private final RecusaEtapaEmailService avisos = mock(RecusaEtapaEmailService.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final ArquivoStorageService storage = mock(ArquivoStorageService.class);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private final DisparoEtapaService disparo =
            new DisparoEtapaService(etapas, recursos, convites, historico, clock);
    private final AceiteEtapaService aceite =
            new AceiteEtapaService(etapas, recursoAutenticado, historico, clock);
    private final EntregaEtapaService entrega =
            new EntregaEtapaService(etapas, recursoAutenticado, storage, historico, clock);
    private final RecusaEtapaService recusa =
            new RecusaEtapaService(etapas, recursoAutenticado, avisos, historico, clock);

    private final UUID osId = UUID.randomUUID();
    private final UUID etapa2Id = UUID.randomUUID();
    private final UsuarioAutenticado gestor = new UsuarioAutenticado("gestor@stentio.com", false);
    private final UsuarioAutenticado recursoLogado =
            new UsuarioAutenticado("bruno@traducoes.com", UUID.randomUUID(), false);

    private Recurso bruno;
    private EtapaOrdemServico etapa1;
    private EtapaOrdemServico etapa2;

    @BeforeEach
    void preparar() {
        Idioma portugues = idioma();
        Idioma ingles = idioma();
        TipoServico traducao = new TipoServico();
        traducao.setId(UUID.randomUUID());
        traducao.setNome("Tradução");

        Solicitacao solicitacao = mock(Solicitacao.class);
        when(solicitacao.getIdiomaOrigem()).thenReturn(portugues);
        when(solicitacao.getIdiomaDestino()).thenReturn(ingles);
        OrdemServico ordem = mock(OrdemServico.class);
        when(ordem.getId()).thenReturn(osId);
        when(ordem.getSolicitacao()).thenReturn(solicitacao);

        etapa1 = new EtapaOrdemServico(ordem, 1, "Tradução", traducao, 24);
        etapa2 = new EtapaOrdemServico(ordem, 2, "Revisão", traducao, 12);
        ReflectionTestUtils.setField(etapa2, "id", etapa2Id);

        bruno = mock(Recurso.class);
        when(bruno.getId()).thenReturn(UUID.randomUUID());
        when(bruno.elegivelParaAlocacao()).thenReturn(true);
        when(bruno.atendePar(any(), any())).thenReturn(true);
        when(bruno.executa(any())).thenReturn(true);
        when(recursoAutenticado.resolver(recursoLogado)).thenReturn(bruno);

        when(etapas.buscarParaAlocacao(osId, etapa2Id)).thenReturn(Optional.of(etapa2));
        when(etapas.buscarParaAceite(etapa2Id)).thenReturn(Optional.of(etapa2));
        // O que o banco responderia: a etapa 2 depende da etapa 1, ainda não entregue.
        when(etapas.anteriorDe(etapa2)).thenReturn(etapa1);
        when(recursos.buscarDisponiveis(any(), any(), any())).thenReturn(List.of(bruno));
    }

    private static Idioma idioma() {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        return i;
    }

    @Test
    @DisplayName("Cenário 1: Gestor tenta disparar a etapa 2 antes da etapa 1 — 409, sem convite nem histórico")
    void disparoForaDeOrdem() {
        assertThrows(TransicaoEtapaInvalidaException.class, () -> disparo.disparar(osId, etapa2Id, gestor));

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, etapa2.getStatus());
        verify(convites, never()).convidar(any(), any(), any());
        verify(historico, never()).save(any());
        verify(etapas, never()).save(any());
    }

    @Test
    @DisplayName("Cenário 1: o 409 da ordem vem antes da busca de elegíveis")
    void ordemAntesDeElegiveis() {
        when(recursos.buscarDisponiveis(any(), any(), any())).thenReturn(List.of());

        // Sem elegíveis seria 400; fora de ordem, o problema a reportar é a ordem.
        assertThrows(TransicaoEtapaInvalidaException.class, () -> disparo.disparar(osId, etapa2Id, gestor));
    }

    @Test
    @DisplayName("Cenário 1: recurso da etapa 2 tenta entregar antes da etapa 1 — 409, sem gravar arquivo")
    void entregaForaDeOrdem() {
        etapa2.alocar(bruno, PRAZO, AGORA);
        etapa2.aceitarPor(etapa1, bruno, AGORA);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "revisao.docx",
                "application/octet-stream", new byte[]{1, 2, 3});

        assertThrows(TransicaoEtapaInvalidaException.class,
                () -> entrega.entregar(etapa2Id, arquivo, recursoLogado));

        assertEquals(StatusEtapaOrdemServico.ACEITA, etapa2.getStatus());
        verify(storage, never()).salvarTodos(any());
        verify(historico, never()).save(any());
    }

    @Test
    @DisplayName("CA.1: convite aberto da etapa 2 não é aceito antes da etapa 1 ser entregue")
    void aceiteDeConviteAbertoForaDeOrdem() {
        ReflectionTestUtils.setField(etapa2, "status", StatusEtapaOrdemServico.DISPARADA);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> aceite.aceitar(etapa2Id, recursoLogado));

        verify(etapas, never()).aceitarSeDisponivel(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CA.1: convite nominal pode ser aceito com antecedência")
    void aceiteNominalComAntecedencia() {
        etapa2.alocar(bruno, PRAZO, AGORA);
        when(etapas.aceitarSeDisponivel(any(), any(), any(), any(), any())).thenReturn(1);

        aceite.aceitar(etapa2Id, recursoLogado);

        verify(etapas).aceitarSeDisponivel(etapa2Id, bruno, AGORA,
                StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ALOCADO);
    }

    @Test
    @DisplayName("RN.1: recusa de etapa não liberada volta para AGUARDANDO_ETAPA_ANTERIOR")
    void recusaNaoLiberada() {
        etapa2.alocar(bruno, PRAZO, AGORA);

        recusa.recusar(etapa2Id, recursoLogado);

        assertEquals(StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR, etapa2.getStatus());
    }

    @Test
    @DisplayName("RN.1: recusa de etapa liberada volta para PENDENTE")
    void recusaLiberada() {
        etapa1.disparar(null, AGORA);
        etapa1.aceitarPor(null, bruno, AGORA);
        etapa1.entregar(null, "etapa1.docx", "2026/11/etapa1.docx", AGORA);
        etapa2.alocar(bruno, PRAZO, AGORA);

        recusa.recusar(etapa2Id, recursoLogado);

        assertEquals(StatusEtapaOrdemServico.PENDENTE, etapa2.getStatus());
    }
}
