package com.stentio.core.service;

import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// US-023 CA.5 e CA.7: o profissional vê as próprias demandas, e só elas.
class PortalRecursoServiceTest {

    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final EtapaOrdemServicoRepository etapas = mock(EtapaOrdemServicoRepository.class);

    private final PortalRecursoService service =
            new PortalRecursoService(
                    new RecursoAutenticadoService(recursos),
                    etapas,
                    new RecursoMapper(),
                    new EtapaLiberacaoService(etapas));

    private final UUID usuarioId = UUID.randomUUID();
    private final UsuarioAutenticado maria = new UsuarioAutenticado("maria@exemplo.com", usuarioId, false);

    private Recurso perfilCadastrado() {
        Recurso recurso = new Recurso("Maria Tradutora", "maria@exemplo.com", null);
        recurso.vincularUsuario(usuarioId);
        recurso.definirVerificacao(true);
        when(recursos.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(recurso));
        return recurso;
    }

    private static EtapaOrdemServico etapaDe(String nome) {
        return etapaDe(nome, 1);
    }

    private static EtapaOrdemServico etapaDe(String nome, int numeroOrdem) {
        Idioma portugues = mock(Idioma.class);
        when(portugues.getNome()).thenReturn("Português");
        Idioma ingles = mock(Idioma.class);
        when(ingles.getNome()).thenReturn("Inglês");

        Solicitacao solicitacao = mock(Solicitacao.class);
        when(solicitacao.getIdiomaOrigem()).thenReturn(portugues);
        when(solicitacao.getIdiomaDestino()).thenReturn(ingles);

        OrdemServico ordem = mock(OrdemServico.class);
        when(ordem.getId()).thenReturn(UUID.randomUUID());
        when(ordem.getCriadoEm()).thenReturn(Instant.parse("2026-11-17T12:00:00Z"));
        when(ordem.getSolicitacao()).thenReturn(solicitacao);

        TipoServico traducao = new TipoServico();
        traducao.setId(UUID.randomUUID());
        traducao.setNome("Tradução");
        traducao.setAtivo(true);

        return new EtapaOrdemServico(ordem, numeroOrdem, nome, traducao, 24);
    }

    @Test
    @DisplayName("CA.5: lista as demandas com o par de idiomas e o código da OS")
    void listaAsDemandas() {
        // A etapa é montada antes do when: etapaDe cria e configura mocks, e fazer isso
        // dentro do thenReturn abriria uma configuração nova com a anterior ainda em curso.
        // O Mockito recusa com UnfinishedStubbingException, e com razão.
        EtapaOrdemServico etapa = etapaDe("Tradução Principal");

        perfilCadastrado();
        when(etapas.listarPorRecurso(any())).thenReturn(List.of(etapa));

        List<DemandaRecursoResponse> demandas = service.minhasDemandas(maria);

        assertEquals(1, demandas.size());
        DemandaRecursoResponse demanda = demandas.getFirst();
        assertEquals("Tradução Principal", demanda.nome());
        assertEquals("Português", demanda.idiomaOrigem());
        assertEquals("Inglês", demanda.idiomaDestino());
        assertEquals("Tradução", demanda.tipoServico());
        assertEquals(StatusEtapaOrdemServico.PENDENTE, demanda.status());
        assertTrue(demanda.liberada());
        assertTrue(demanda.ordemServicoCodigo().startsWith("OS-2026-"));
    }

    @Test
    @DisplayName("A etapa alocada informa se a etapa anterior já foi entregue")
    void informaLiberacaoDaEtapaAlocada() {
        EtapaOrdemServico etapa = etapaDe("Revisão", 2);
        Recurso recursoAlocado = mock(Recurso.class);
        when(recursoAlocado.isAtivo()).thenReturn(true);
        etapa.alocar(
                recursoAlocado,
                Instant.parse("2026-11-22T12:00:00Z"),
                Instant.parse("2026-11-21T12:00:00Z"),
                true);
        UUID ordemId = etapa.getOrdemServico().getId();
        perfilCadastrado();
        when(etapas.listarPorRecurso(any())).thenReturn(List.of(etapa));
        when(etapas.buscarStatusPorOrdem(ordemId, 1))
                .thenReturn(Optional.of(StatusEtapaOrdemServico.PENDENTE));

        DemandaRecursoResponse demandaBloqueada = service.minhasDemandas(maria).getFirst();

        assertEquals(StatusEtapaOrdemServico.ALOCADO, demandaBloqueada.status());
        assertFalse(demandaBloqueada.liberada());

        when(etapas.buscarStatusPorOrdem(ordemId, 1))
                .thenReturn(Optional.of(StatusEtapaOrdemServico.ENTREGUE));

        DemandaRecursoResponse demandaLiberada = service.minhasDemandas(maria).getFirst();

        assertTrue(demandaLiberada.liberada());
    }

    @Test
    @DisplayName("CA.6: lista convites abertos e etapas alocadas com seus status atuais")
    void listaConvitesEEtapasAlocadas() {
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");
        EtapaOrdemServico alocada = etapaDe("Revisão");
        Recurso recursoAlocado = mock(Recurso.class);
        when(recursoAlocado.isAtivo()).thenReturn(true);
        alocada.alocar(recursoAlocado, agora.plusSeconds(3600), agora, true);

        EtapaOrdemServico conviteAberto = etapaDe("Tradução");
        conviteAberto.disparar(agora, true);

        perfilCadastrado();
        when(etapas.listarPorRecurso(any())).thenReturn(List.of(alocada));
        when(etapas.listarDisponiveisPara(any(), eq(StatusEtapaOrdemServico.DISPARADA)))
                .thenReturn(List.of(conviteAberto));

        List<DemandaRecursoResponse> demandas = service.minhasDemandas(maria);

        assertEquals(2, demandas.size());
        assertEquals(StatusEtapaOrdemServico.ALOCADO, demandas.getFirst().status());
        assertEquals(StatusEtapaOrdemServico.DISPARADA, demandas.getLast().status());
    }

    @Test
    @DisplayName("CA.7: o recurso é resolvido pelo usuarioId do token, e por nada mais")
    void resolveOProprioRecursoPeloToken() {
        perfilCadastrado();
        when(etapas.listarPorRecurso(any())).thenReturn(List.of());

        service.minhasDemandas(maria);

        // Não há caminho por onde um id venha de fora: a única chave é a do token.
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(recursos).buscarPorUsuario(captor.capture());
        assertEquals(usuarioId, captor.getValue());
        verify(etapas).listarPorRecurso(any());
    }

    @Test
    @DisplayName("Conta sem perfil recebe uma explicação, não uma lista vazia")
    void contaSemPerfil() {
        when(recursos.buscarPorUsuario(usuarioId)).thenReturn(Optional.empty());

        var erro = assertThrows(AutoCadastroInvalidoException.class, () -> service.minhasDemandas(maria));

        assertTrue(erro.getMessage().contains("Conclua o cadastro"));
    }

    @Test
    @DisplayName("RN.3: sessão sem userId pede login novo")
    void sessaoSemUsuarioId() {
        UsuarioAutenticado tokenAntigo = new UsuarioAutenticado("maria@exemplo.com", false);

        var erro = assertThrows(AutoCadastroInvalidoException.class,
                () -> service.minhasDemandas(tokenAntigo));

        assertTrue(erro.getMessage().contains("Entre novamente"));
        verifyNoInteractions(etapas);
    }

    @Test
    @DisplayName("Meu perfil devolve as competências do próprio recurso")
    void meuPerfil() {
        perfilCadastrado();

        var perfil = service.meuPerfil(maria);

        assertEquals("Maria Tradutora", perfil.nome());
        assertTrue(perfil.verificado());
    }
}
