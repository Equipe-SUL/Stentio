package com.stentio.core.service;

import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.AlocacaoEtapaResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.AlocacaoNaoPermitidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.EventoEtapa;
import com.stentio.core.model.HistoricoEtapa;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.TipoServico;
import com.stentio.core.model.UnidadeCobranca;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.HistoricoEtapaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// US-012: compatibilidade (RN.1 e RN.2), permissão de troca (RN.3) e o rastro da CA.4.
class AlocacaoEtapaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T12:00:00Z");
    private static final Instant DAQUI_A_UMA_SEMANA = Instant.parse("2026-11-16T12:00:00Z");

    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);
    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final HistoricoEtapaRepository historico = mock(HistoricoEtapaRepository.class);

    private final org.springframework.context.ApplicationEventPublisher eventos =
            mock(org.springframework.context.ApplicationEventPublisher.class);

    private final CertificacaoConsultaService certificacoes = mock(CertificacaoConsultaService.class);

    private final AlocacaoEtapaService service = new AlocacaoEtapaService(
            etapas, recursos, historico, certificacoes, Clock.fixed(AGORA, ZoneOffset.UTC), eventos);

    private final UUID osId = UUID.randomUUID();
    private final UUID etapaId = UUID.randomUUID();

    private final UsuarioAutenticado gestor = new UsuarioAutenticado("gestor@stentio.com", false);
    private final UsuarioAutenticado admin = new UsuarioAutenticado("admin@stentio.com", true);

    private Idioma portugues;
    private Idioma ingles;
    private Idioma alemao;
    private TipoServico traducao;
    private TipoServico revisao;
    private EtapaOrdemServico etapa;

    @BeforeEach
    void preparar() {
        // US-024: o caso comum é o profissional certificado para o par. Os testes
        // que exercitam a falta de certificação sobrescrevem este stub.
        when(certificacoes.certificadoParaAEtapa(any(), any())).thenReturn(true);
        portugues = idioma("Português");
        ingles = idioma("Inglês");
        alemao = idioma("Alemão");
        traducao = tipo("Tradução");
        revisao = tipo("Revisão");

        etapa = etapaDe(traducao);
        when(etapas.buscarParaAlocacao(osId, etapaId)).thenReturn(Optional.of(etapa));
        when(etapas.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Idioma idioma(String nome) {
        Idioma i = mock(Idioma.class);
        when(i.getId()).thenReturn(UUID.randomUUID());
        when(i.getNome()).thenReturn(nome);
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
        when(ordem.getId()).thenReturn(osId);
        when(ordem.getSolicitacao()).thenReturn(solicitacao);

        EtapaOrdemServico nova = new EtapaOrdemServico(ordem, 1, "Tradução Principal", tipoDaEtapa, 24);
        // Etapa carregada do banco sempre tem id; o evento de auditoria (US-019) depende dele.
        org.springframework.test.util.ReflectionTestUtils.setField(nova, "id", etapaId);
        return nova;
    }

    private Recurso recurso(String nome, TipoServico tipoHabilitado, Idioma origem, Idioma destino) {
        Recurso r = new Recurso(nome, nome.toLowerCase().replace(" ", ".") + "@traducoes.com", null);
        // US-023 RN.5: profissional só é alocável depois de verificado pelo gestor.
        r.definirVerificacao(true);
        r.definirTiposServico(List.of(tipoHabilitado));
        r.definirPrecos(List.of(new Recurso.NovoPreco(
                origem, destino, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
        return r;
    }

    /** Registra o recurso no repositório falso e devolve o id pelo qual o service o acha. */
    private UUID idRegistradoDe(Recurso r) {
        UUID id = UUID.randomUUID();
        when(recursos.buscarComCompetencias(id)).thenReturn(Optional.of(r));
        return id;
    }

    private AlocacaoEtapaResponse alocar(UUID recursoId, UsuarioAutenticado ator) {
        return service.alocar(osId, etapaId,
                new AlocacaoEtapaRequest(recursoId, DAQUI_A_UMA_SEMANA), ator);
    }

    @Test
    @DisplayName("Cenário 1: aloca recurso compatível, grava a data limite e devolve situação ALOCADO")
    void alocaRecursoCompativel() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));

        AlocacaoEtapaResponse resposta = alocar(anaId, gestor);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, resposta.statusEtapa());
        assertEquals("Ana Silva", resposta.recurso().nome());
        assertEquals(DAQUI_A_UMA_SEMANA, resposta.dataLimiteEntrega());
        assertEquals(DAQUI_A_UMA_SEMANA, etapa.getDataLimiteEntrega());
        assertEquals(AGORA, etapa.getAlocadoEm());
        assertTrue(etapa.alocada());
    }

    @Test
    @DisplayName("CA.4: a alocação deixa rastro com quem alocou e quando")
    void gravaHistoricoDaAlocacao() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));

        alocar(anaId, gestor);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico).save(captor.capture());

        HistoricoEtapa registro = captor.getValue();
        assertEquals(EventoEtapa.ALOCACAO, registro.getEvento());
        assertNull(registro.getRecursoAnterior());
        assertEquals("Ana Silva", registro.getRecursoNovo().getNome());
        assertEquals("gestor@stentio.com", registro.getRealizadoPor());
        assertEquals(AGORA, registro.getRealizadoEm());
    }

    @Test
    @DisplayName("Trocar o profissional entra como REALOCACAO, guardando quem estava antes")
    void realocacaoGuardaORecursoAnterior() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));
        UUID brunoId = idRegistradoDe(recurso("Bruno Lima", traducao, portugues, ingles));

        alocar(anaId, gestor);
        alocar(brunoId, gestor);

        ArgumentCaptor<HistoricoEtapa> captor = ArgumentCaptor.forClass(HistoricoEtapa.class);
        verify(historico, times(2)).save(captor.capture());

        HistoricoEtapa troca = captor.getAllValues().get(1);
        assertEquals(EventoEtapa.REALOCACAO, troca.getEvento());
        assertEquals("Ana Silva", troca.getRecursoAnterior().getNome());
        assertEquals("Bruno Lima", troca.getRecursoNovo().getNome());
        assertEquals("Bruno Lima", etapa.getRecurso().getNome());
    }

    @Test
    @DisplayName("Cenário 2 e RN.1: recurso sem o par de idiomas é recusado com 400 nomeando o par")
    void recusaRecursoSemOParDeIdiomas() {
        UUID carlosId = idRegistradoDe(recurso("Carlos Dias", traducao, portugues, alemao));

        var erro = assertThrows(AlocacaoInvalidaException.class, () -> alocar(carlosId, gestor));

        assertTrue(erro.getMessage().contains("Português"));
        assertTrue(erro.getMessage().contains("Inglês"));
        assertFalse(etapa.alocada());
        verifyNoInteractions(historico);
    }

    @Test
    @DisplayName("RN.2: recurso sem o tipo de serviço da etapa é recusado nomeando o tipo")
    void recusaRecursoSemOTipoDeServico() {
        UUID danielaId = idRegistradoDe(recurso("Daniela Reis", revisao, portugues, ingles));

        var erro = assertThrows(AlocacaoInvalidaException.class, () -> alocar(danielaId, gestor));

        assertTrue(erro.getMessage().contains("Tradução"));
        assertFalse(etapa.alocada());
    }

    @Test
    @DisplayName("Recurso desativado não recebe etapa nova")
    void recusaRecursoInativo() {
        Recurso ana = recurso("Ana Silva", traducao, portugues, ingles);
        ana.desativar();
        UUID anaId = idRegistradoDe(ana);

        var erro = assertThrows(AlocacaoInvalidaException.class, () -> alocar(anaId, gestor));

        assertTrue(erro.getMessage().contains("inativo"));
    }

    @Test
    @DisplayName("US-024 RN.5: o verificado deixou de barrar a alocação; quem barra é a certificação")
    void verificadoNaoDecideMais() {
        Recurso novato = recurso("Elisa Prado", traducao, portugues, ingles);
        novato.definirVerificacao(false);
        UUID elisaId = idRegistradoDe(novato);

        // Certificada para o par, mesmo sem o "cadastro revisado": a alocação passa.
        AlocacaoEtapaResponse resposta = alocar(elisaId, gestor);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, resposta.statusEtapa());
        assertTrue(etapa.alocada());
    }

    @Test
    @DisplayName("CA.3: data limite no passado é recusada")
    void recusaDataLimiteNoPassado() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));

        var erro = assertThrows(AlocacaoInvalidaException.class, () -> service.alocar(osId, etapaId,
                new AlocacaoEtapaRequest(anaId, AGORA.minusSeconds(60)), gestor));

        assertTrue(erro.getMessage().contains("futuro"));
        assertFalse(etapa.alocada());
    }

    @Test
    @DisplayName("Etapa inexistente, ou de outra ordem de serviço, responde não encontrada")
    void etapaInexistente() {
        UUID outra = UUID.randomUUID();
        when(etapas.buscarParaAlocacao(osId, outra)).thenReturn(Optional.empty());

        assertThrows(EtapaNaoEncontradaException.class, () -> service.alocar(osId, outra,
                new AlocacaoEtapaRequest(UUID.randomUUID(), DAQUI_A_UMA_SEMANA), gestor));
    }

    @Test
    @DisplayName("Recurso inexistente responde não encontrado")
    void recursoInexistente() {
        UUID fantasma = UUID.randomUUID();
        when(recursos.buscarComCompetencias(fantasma)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> alocar(fantasma, gestor));
    }

    @Test
    @DisplayName("RN.3: gestor não troca o recurso de etapa já iniciada")
    void rn3BloqueiaGestorEmEtapaIniciada() {
        EtapaOrdemServico iniciada = spy(etapaDe(traducao));
        doReturn(true).when(iniciada).iniciada();
        when(etapas.buscarParaAlocacao(osId, etapaId)).thenReturn(Optional.of(iniciada));
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));

        var erro = assertThrows(AlocacaoNaoPermitidaException.class, () -> alocar(anaId, gestor));

        assertTrue(erro.getMessage().contains("administrativo"));
        assertFalse(iniciada.alocada());
        verifyNoInteractions(historico);
    }

    @Test
    @DisplayName("RN.3: o administrador troca o recurso mesmo com a etapa iniciada")
    void rn3PermiteAdministrador() {
        EtapaOrdemServico iniciada = spy(etapaDe(traducao));
        doReturn(true).when(iniciada).iniciada();
        when(etapas.buscarParaAlocacao(osId, etapaId)).thenReturn(Optional.of(iniciada));
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));

        AlocacaoEtapaResponse resposta = alocar(anaId, admin);

        assertEquals(StatusEtapaOrdemServico.ALOCADO, resposta.statusEtapa());
        assertTrue(iniciada.alocada());
    }

    @Test
    @DisplayName("Enquanto a etapa não começou, trocar o recurso é livre para o gestor")
    void trocaLivreAntesDeComecar() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));
        UUID brunoId = idRegistradoDe(recurso("Bruno Lima", traducao, portugues, ingles));

        alocar(anaId, gestor);
        AlocacaoEtapaResponse resposta = alocar(brunoId, gestor);

        assertEquals("Bruno Lima", resposta.recurso().nome());
    }

    @Test
    @DisplayName("US-024 Cenário 4: sem certificação aprovada do par, o gestor não consegue alocar")
    void naoAlocaSemCertificacaoDoPar() {
        UUID anaId = idRegistradoDe(recurso("Ana Silva", traducao, portugues, ingles));
        when(certificacoes.certificadoParaAEtapa(any(), any())).thenReturn(false);

        var erro = assertThrows(AlocacaoInvalidaException.class, () -> alocar(anaId, gestor));

        assertTrue(erro.getMessage().contains("certificação"));
        verify(etapas, never()).save(any());
    }
}
