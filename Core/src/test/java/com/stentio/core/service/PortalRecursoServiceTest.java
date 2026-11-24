package com.stentio.core.service;

import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.ArquivoSolicitacao;
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
import org.springframework.test.util.ReflectionTestUtils;

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
        // O id normalmente vem do banco; "a etapa é dela?" compara ids.
        ReflectionTestUtils.setField(recurso, "id", UUID.randomUUID());
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
        etapa.alocar(
                perfilCadastrado(),
                Instant.parse("2026-11-22T12:00:00Z"),
                Instant.parse("2026-11-21T12:00:00Z"),
                true);
        UUID ordemId = etapa.getOrdemServico().getId();
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
        alocada.alocar(perfilCadastrado(), agora.plusSeconds(3600), agora, true);

        EtapaOrdemServico conviteAberto = etapaDe("Tradução");
        conviteAberto.disparar(agora, true);

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

    // ------------------------------------------------------------------ anexos do cliente

    private static ArquivoSolicitacao anexo(String nome) {
        ArquivoSolicitacao arquivo = mock(ArquivoSolicitacao.class);
        when(arquivo.getId()).thenReturn(UUID.randomUUID());
        when(arquivo.getNomeOriginal()).thenReturn(nome);
        when(arquivo.getCaminho()).thenReturn("2026/11/" + nome);
        when(arquivo.getTamanhoBytes()).thenReturn(2048L);
        return arquivo;
    }

    private EtapaOrdemServico etapaDaMariaCom(ArquivoSolicitacao... anexos) {
        EtapaOrdemServico etapa = etapaDe("Tradução Principal");
        ReflectionTestUtils.setField(etapa, "id", UUID.randomUUID());
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        when(solicitacao.getArquivos()).thenReturn(List.of(anexos));
        etapa.alocar(perfilCadastrado(), Instant.parse("2026-11-22T12:00:00Z"),
                Instant.parse("2026-11-21T12:00:00Z"), true);
        return etapa;
    }

    @Test
    @DisplayName("Etapa alocada ao profissional traz os anexos do cliente com a rota de download")
    void etapaAlocadaTrazAnexos() {
        ArquivoSolicitacao contrato = anexo("contrato.pdf");
        EtapaOrdemServico etapa = etapaDaMariaCom(contrato);
        when(etapas.listarPorRecurso(any())).thenReturn(List.of(etapa));

        DemandaRecursoResponse demanda = service.minhasDemandas(maria).getFirst();

        assertEquals(1, demanda.arquivos().size());
        DemandaRecursoResponse.ArquivoDaSolicitacao arquivo = demanda.arquivos().getFirst();
        assertEquals("contrato.pdf", arquivo.nome());
        assertEquals(2048L, arquivo.tamanhoBytes());
        assertEquals("/api/v1/recurso/etapas/" + etapa.getId() + "/arquivos/" + contrato.getId(), arquivo.url());
    }

    @Test
    @DisplayName("Convite aberto não traz anexos: o documento vai para quem assumiu a etapa")
    void conviteAbertoSemAnexos() {
        ArquivoSolicitacao contrato = anexo("contrato.pdf");
        EtapaOrdemServico conviteAberto = etapaDe("Tradução");
        Solicitacao solicitacao = conviteAberto.getOrdemServico().getSolicitacao();
        when(solicitacao.getArquivos()).thenReturn(List.of(contrato));
        conviteAberto.disparar(Instant.parse("2026-11-20T12:00:00Z"), true);
        perfilCadastrado();
        when(etapas.listarDisponiveisPara(any(), eq(StatusEtapaOrdemServico.DISPARADA)))
                .thenReturn(List.of(conviteAberto));

        DemandaRecursoResponse demanda = service.minhasDemandas(maria).getFirst();

        assertEquals(StatusEtapaOrdemServico.DISPARADA, demanda.status());
        assertTrue(demanda.arquivos().isEmpty());
    }

    @Test
    @DisplayName("Download: a dona da etapa baixa o anexo da solicitação")
    void donaBaixaOAnexo() {
        ArquivoSolicitacao contrato = anexo("contrato.pdf");
        EtapaOrdemServico etapa = etapaDaMariaCom(contrato);
        when(etapas.buscarParaAceite(etapa.getId())).thenReturn(Optional.of(etapa));

        PortalRecursoService.AnexoParaDownload arquivo =
                service.localizarAnexo(etapa.getId(), contrato.getId(), maria);

        assertEquals("contrato.pdf", arquivo.nomeOriginal());
        assertEquals("2026/11/contrato.pdf", arquivo.caminho());
    }

    @Test
    @DisplayName("Download: etapa de outro profissional é 404, sem revelar o arquivo")
    void outroProfissionalNaoBaixa() {
        ArquivoSolicitacao contrato = anexo("contrato.pdf");
        EtapaOrdemServico etapa = etapaDaMariaCom(contrato);
        when(etapas.buscarParaAceite(etapa.getId())).thenReturn(Optional.of(etapa));

        UUID outroUsuario = UUID.randomUUID();
        Recurso joao = new Recurso("João", "joao@exemplo.com", null);
        ReflectionTestUtils.setField(joao, "id", UUID.randomUUID());
        joao.vincularUsuario(outroUsuario);
        when(recursos.buscarPorUsuario(outroUsuario)).thenReturn(Optional.of(joao));
        UsuarioAutenticado tokenDoJoao = new UsuarioAutenticado("joao@exemplo.com", outroUsuario, false);

        assertThrows(EtapaNaoEncontradaException.class,
                () -> service.localizarAnexo(etapa.getId(), contrato.getId(), tokenDoJoao));
    }

    @Test
    @DisplayName("Download: arquivo de outra solicitação não sai por esta etapa")
    void arquivoDeOutraSolicitacao() {
        EtapaOrdemServico etapa = etapaDaMariaCom(anexo("contrato.pdf"));
        when(etapas.buscarParaAceite(etapa.getId())).thenReturn(Optional.of(etapa));

        assertThrows(EtapaNaoEncontradaException.class,
                () -> service.localizarAnexo(etapa.getId(), UUID.randomUUID(), maria));
    }
}
