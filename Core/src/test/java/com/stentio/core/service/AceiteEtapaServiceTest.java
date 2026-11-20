package com.stentio.core.service;

import com.stentio.core.dto.AceiteEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
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
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-013 CA.3, CA.4 e RN.3: quem pode aceitar, e o que acontece quando dois tentam. */
class AceiteEtapaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");
    private static final Instant PRAZO = Instant.parse("2026-11-24T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final RecursoAutenticadoService recursoAutenticado = mock(RecursoAutenticadoService.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);

    private final AceiteEtapaService service = new AceiteEtapaService(
            etapas, recursoAutenticado, historico, Clock.fixed(AGORA, ZoneOffset.UTC));

    private final UUID etapaId = UUID.randomUUID();
    private final UsuarioAutenticado ator = new UsuarioAutenticado("ana@traducoes.com", UUID.randomUUID(), false);

    private Idioma portugues;
    private Idioma ingles;
    private Idioma alemao;
    private TipoServico traducao;
    private TipoServico revisao;
    private EtapaOrdemServico etapa;
    private Recurso ana;

    @BeforeEach
    void preparar() {
        portugues = idioma();
        ingles = idioma();
        alemao = idioma();
        traducao = tipo("Tradução");
        revisao = tipo("Revisão");

        etapa = etapaDe(traducao);
        ana = recurso("Ana Silva", traducao, portugues, ingles);

        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.of(etapa));
        when(recursoAutenticado.resolver(ator)).thenReturn(ana);
    }

    private static Idioma idioma() {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        return i;
    }

    private static TipoServico tipo(String nome) {
        TipoServico t = new TipoServico();
        t.setId(UUID.randomUUID());
        t.setNome(nome);
        t.setAtivo(true);
        return t;
    }

    /** Etapa de uma OS cuja solicitação é Português → Inglês. */
    private EtapaOrdemServico etapaDe(TipoServico tipoDaEtapa) {
        Solicitacao solicitacao = mock(Solicitacao.class);
        when(solicitacao.getIdiomaOrigem()).thenReturn(portugues);
        when(solicitacao.getIdiomaDestino()).thenReturn(ingles);

        OrdemServico ordem = mock(OrdemServico.class);
        when(ordem.getSolicitacao()).thenReturn(solicitacao);

        return new EtapaOrdemServico(ordem, 1, "Tradução Principal", tipoDaEtapa, 24);
    }

    private static Recurso recurso(String nome, TipoServico habilitado, Idioma origem, Idioma destino) {
        Recurso r = new Recurso(nome, nome.toLowerCase().replace(" ", ".") + "@traducoes.com", null);
        // O id normalmente vem do banco; pertenceA compara ids, então aqui ele é posto à mão.
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        r.definirVerificacao(true);
        r.definirTiposServico(List.of(habilitado));
        r.definirPrecos(List.of(new Recurso.NovoPreco(
                origem, destino, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
        return r;
    }

    /** Faz o UPDATE condicional se comportar como o banco: aplica a mudança e devolve 1. */
    private void bancoAceitaOClaim() {
        when(etapas.aceitarDisparada(eq(etapaId), any(), any(), any(), any()))
                .thenAnswer(invocacao -> {
                    etapa.aceitarPor(invocacao.getArgument(1), invocacao.getArgument(2));
                    return 1;
                });
        when(etapas.aceitarAlocacaoDireta(eq(etapaId), any(), any(), any(), any()))
                .thenAnswer(invocacao -> {
                    etapa.aceitarPor(invocacao.getArgument(1), invocacao.getArgument(2));
                    return 1;
                });
    }

    @Test
    @DisplayName("CA.3: aceita a etapa disparada e devolve o estado novo")
    void aceitaEtapaDisparada() {
        etapa.disparar(AGORA);
        bancoAceitaOClaim();

        AceiteEtapaResponse resposta = service.aceitar(etapaId, ator);

        assertEquals(StatusEtapaOrdemServico.ACEITA, resposta.status());
        assertEquals(AGORA, resposta.aceitoEm());
        verify(etapas).aceitarDisparada(etapaId, ana, AGORA,
                StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.DISPARADA);
    }

    @Test
    @DisplayName("CA.3: quem perde a corrida atualiza zero linhas e recebe conflito")
    void segundoAceiteFalha() {
        etapa.disparar(AGORA);
        when(etapas.aceitarDisparada(eq(etapaId), any(), any(), any(), any())).thenReturn(0);

        assertThrows(TransicaoEtapaInvalidaException.class, () -> service.aceitar(etapaId, ator));
        verify(historico, never()).save(any());
    }

    @Test
    @DisplayName("CA.8: o aceite vencedor deixa rastro com quem aceitou")
    void gravaHistoricoDoAceite() {
        etapa.disparar(AGORA);
        bancoAceitaOClaim();

        service.aceitar(etapaId, ator);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico).save(captor.capture());
        assertEquals(EventoEtapa.ACEITE, captor.getValue().getEvento());
        assertEquals("ana@traducoes.com", captor.getValue().getRealizadoPor());
    }

    @Test
    @DisplayName("CA.4: perfil que não atende o par de idiomas não aceita")
    void recusaParDeIdiomasIncompativel() {
        etapa.disparar(AGORA);
        // Construído fora do when: montar o recurso toca nos mocks de Idioma, e fazer isso
        // dentro do thenReturn deixa a stubbing inacabada (UnfinishedStubbingException).
        Recurso bruno = recurso("Bruno", traducao, portugues, alemao);
        when(recursoAutenticado.resolver(ator)).thenReturn(bruno);

        assertThrows(AlocacaoInvalidaException.class, () -> service.aceitar(etapaId, ator));
        verify(etapas, never()).aceitarDisparada(any(), any(), any(), any(), any());
        verify(etapas, never()).aceitarAlocacaoDireta(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CA.4: perfil que não executa o tipo de serviço não aceita")
    void recusaTipoDeServicoIncompativel() {
        etapa.disparar(AGORA);
        Recurso carla = recurso("Carla", revisao, portugues, ingles);
        when(recursoAutenticado.resolver(ator)).thenReturn(carla);

        assertThrows(AlocacaoInvalidaException.class, () -> service.aceitar(etapaId, ator));
    }

    @Test
    @DisplayName("US-023 RN.5: cadastro ainda não verificado não aceita nada")
    void recusaRecursoNaoVerificado() {
        etapa.disparar(AGORA);
        Recurso semVerificacao = recurso("Diego", traducao, portugues, ingles);
        semVerificacao.definirVerificacao(false);
        when(recursoAutenticado.resolver(ator)).thenReturn(semVerificacao);

        assertThrows(AlocacaoInvalidaException.class, () -> service.aceitar(etapaId, ator));
    }

    @Test
    @DisplayName("CA.6: convite nominal de outra pessoa não é aceitável, mesmo com perfil compatível")
    void naoAceitaConviteDeOutro() {
        etapa.alocar(recurso("Elisa", traducao, portugues, ingles), PRAZO, AGORA);

        assertThrows(AlocacaoInvalidaException.class, () -> service.aceitar(etapaId, ator));
        verify(etapas, never()).aceitarDisparada(any(), any(), any(), any(), any());
        verify(etapas, never()).aceitarAlocacaoDireta(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CA.6: o convite nominal é aceito com a condição ALOCADO, não DISPARADA")
    void aceitaProprioConviteNominal() {
        etapa.alocar(ana, PRAZO, AGORA);
        bancoAceitaOClaim();

        service.aceitar(etapaId, ator);

        verify(etapas).aceitarAlocacaoDireta(etapaId, ana, AGORA,
                StatusEtapaOrdemServico.ACEITA, StatusEtapaOrdemServico.ALOCADO);
    }

    @Test
    @DisplayName("etapa inexistente é 404, não conflito")
    void etapaInexistente() {
        when(etapas.buscarParaAceite(etapaId)).thenReturn(Optional.empty());

        assertThrows(EtapaNaoEncontradaException.class, () -> service.aceitar(etapaId, ator));
    }
}
