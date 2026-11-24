package com.stentio.core.service;

import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.dto.RecursoRequest;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correções do PR #242 (US-013), contra o Postgres real — os dois defeitos só aparecem com
 * SQL de verdade:
 *
 * <ul>
 *   <li>pares de idioma repetidos no perfil: as consultas com {@code join fetch} em
 *       tiposServico e precos devolvem uma linha por serviço × preço, e a lista de preços
 *       recebia uma cópia por serviço;</li>
 *   <li>a tela de alocação listava o catálogo inteiro; a lista de elegíveis da etapa tem de
 *       trazer só quem atende o par de idiomas e o tipo de serviço.</li>
 * </ul>
 *
 * <p>Mesmo opt-in e banco dos demais testes de integração:
 * {@code ORCAMENTO_INTEGRATION_TESTS=true}.
 */
@EnabledIfEnvironmentVariable(named = "ORCAMENTO_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${ORCAMENTO_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/stentio_test}",
        "spring.datasource.username=${ORCAMENTO_TEST_DB_USER:postgres}",
        "spring.datasource.password=${ORCAMENTO_TEST_DB_PASSWORD:postgres}",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoTemplateAutoConfiguration",
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.orcamentos.outbox.enabled=false",
        "app.seed.test-data.enabled=false"
})
class RecursoCompetenciasPostgresTest {

    @Autowired RecursosElegiveisEtapaService elegiveis;
    @Autowired EtapaOrdemServicoService etapasService;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaProjetoRepository categorias;
    @Autowired IdiomaRepository idiomas;
    @Autowired SolicitacaoRepository solicitacoes;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired OrdemServicoRepository ordensServico;
    @Autowired TipoServicoRepository tiposServico;
    @Autowired RecursoRepository recursos;
    @Autowired RecursoService recursoService;
    @Autowired RecursoCertificacaoRepository certificacoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private final String marca = "PR242-" + UUID.randomUUID().toString().substring(0, 8);
    private final List<UUID> recursosCriados = new ArrayList<>();

    private UUID clienteId;
    private UUID categoriaId;
    private UUID espanholId;
    private UUID inglesId;
    private UUID solicitacaoId;
    private UUID orcamentoId;
    private UUID osId;
    private UUID traducaoId;
    private UUID revisaoId;
    private UUID formatacaoId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa " + marca, "Cliente PR242",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "71428793860"));
            clienteId = cliente.getId();

            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria " + marca);
            categoriaId = categorias.saveAndFlush(categoria).getId();

            espanholId = novoIdioma("Espanhol").getId();
            inglesId = novoIdioma("Inglês").getId();

            traducaoId = novoTipo("Tradução");
            revisaoId = novoTipo("Revisão");
            formatacaoId = novoTipo("Formatação");
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (osId != null) {
                jdbc.update("delete from log_auditoria where entidade_id in "
                        + "(select id from etapas_os where os_id = ?) or entidade_id = ?", osId, osId);
                jdbc.update("delete from etapas_os where os_id = ?", osId);
                jdbc.update("delete from ordens_servico where id = ?", osId);
                jdbc.update("delete from orcamentos where id = ?", orcamentoId);
                jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            }
            for (UUID recurso : recursosCriados) {
                jdbc.update("delete from recursos_certificacoes where recurso_id = ?", recurso);
                jdbc.update("delete from recursos_precos where recurso_id = ?", recurso);
                jdbc.update("delete from recursos_tipos_servico where recurso_id = ?", recurso);
                jdbc.update("delete from recursos where id = ?", recurso);
            }
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            jdbc.update("delete from tipos_servico where id in (?, ?, ?)", traducaoId, revisaoId, formatacaoId);
            jdbc.update("delete from idiomas where id in (?, ?)", espanholId, inglesId);
        });
    }

    // ------------------------------------------------------------------ pares repetidos

    @Test
    @DisplayName("Perfil com 2 serviços e 1 par: o par aparece uma vez só (antes aparecia duas)")
    void perfilNaoRepetePares() {
        UUID usuarioId = UUID.randomUUID();
        UUID recursoId = criarRecurso("Álvaro", usuarioId,
                List.of(revisaoId, traducaoId), List.<UUID[]>of(new UUID[]{espanholId, inglesId}));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Recurso perfil = recursos.buscarPorUsuario(usuarioId).orElseThrow();
            assertEquals(2, perfil.getTiposServico().size());
            assertEquals(1, perfil.getPrecos().size(), "o join fetch não pode multiplicar os preços");

            Recurso comCompetencias = recursos.buscarComCompetencias(recursoId).orElseThrow();
            assertEquals(1, comCompetencias.getPrecos().size());
        });
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from recursos_precos where recurso_id = ?", Integer.class, recursoId));
    }

    @Test
    @DisplayName("Perfil com 3 serviços e 2 pares: cada par uma vez (antes, 3 cópias de cada)")
    void perfilComTresServicos() {
        UUID usuarioId = UUID.randomUUID();
        criarRecurso("Beatriz", usuarioId, List.of(revisaoId, traducaoId, formatacaoId),
                List.of(new UUID[]{espanholId, inglesId}, new UUID[]{inglesId, espanholId}));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertEquals(2, recursos.buscarPorUsuario(usuarioId).orElseThrow().getPrecos().size()));
    }

    @Test
    @DisplayName("Editar recurso atualiza tipos de serviço e acrescenta um par de idiomas")
    void editarAtualizaColecoesDeCompetencias() {
        UUID recursoId = criarRecurso("Recurso editado", null, List.of(revisaoId),
                List.<UUID[]>of(new UUID[]{espanholId, inglesId}));
        String email = jdbc.queryForObject(
                "select email from recursos where id = ?", String.class, recursoId);

        RecursoRequest request = new RecursoRequest(
                "Recurso editado " + marca,
                email,
                "11988887777",
                List.of(revisaoId, traducaoId),
                List.of(
                        new RecursoRequest.PrecoRequest(
                                espanholId, inglesId, UnidadeCobranca.PALAVRA, new BigDecimal("0.16")),
                        new RecursoRequest.PrecoRequest(
                                inglesId, espanholId, UnidadeCobranca.PALAVRA, new BigDecimal("0.21"))));

        var resposta = recursoService.editar(recursoId, request);

        assertEquals(2, resposta.tiposServico().size());
        assertEquals(2, resposta.precos().size());
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from recursos_tipos_servico where recurso_id = ?", Integer.class, recursoId));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from recursos_precos where recurso_id = ?", Integer.class, recursoId));
    }

    // ------------------------------------------------------------------ elegíveis da etapa

    @Test
    @DisplayName("Alocação: lista só quem tem o par English → Espanhol certificado e o serviço Formatação da etapa")
    void elegiveisDaEtapa() {
        UUID etapaId = criarOsComEtapa(inglesId, espanholId, formatacaoId);

        // Caso do print: tem o serviço, mas só o par inverso (Espanhol → English).
        UUID alvaro = criarRecurso("Álvaro par invertido", null, List.of(formatacaoId, traducaoId),
                List.<UUID[]>of(new UUID[]{espanholId, inglesId}));
        certificar(alvaro, espanholId, inglesId, StatusCertificacao.APROVADA);
        // Tem o par e a certificação, mas não o serviço.
        UUID bruno = criarRecurso("Bruno sem formatação", null, List.of(traducaoId),
                List.<UUID[]>of(new UUID[]{inglesId, espanholId}));
        certificar(bruno, inglesId, espanholId, StatusCertificacao.APROVADA);
        // US-024: tem o par e o serviço, mas nenhuma certificação.
        criarRecurso("Carla sem certificação", null, List.of(formatacaoId),
                List.<UUID[]>of(new UUID[]{inglesId, espanholId}));
        // US-024: certificação do par ainda em análise.
        UUID diego = criarRecurso("Diego certificação pendente", null, List.of(formatacaoId),
                List.<UUID[]>of(new UUID[]{inglesId, espanholId}));
        certificar(diego, inglesId, espanholId, StatusCertificacao.PENDENTE);
        // US-024: certificado só no par invertido, embora atenda English → Espanhol no cadastro.
        UUID eva = criarRecurso("Eva certificada no par invertido", null, List.of(formatacaoId),
                List.of(new UUID[]{inglesId, espanholId}, new UUID[]{espanholId, inglesId}));
        certificar(eva, espanholId, inglesId, StatusCertificacao.APROVADA);
        // Atende tudo, com a certificação do par aprovada.
        UUID joao = criarRecurso("João habilitado", null, List.of(formatacaoId, revisaoId),
                List.<UUID[]>of(new UUID[]{inglesId, espanholId}));
        certificar(joao, inglesId, espanholId, StatusCertificacao.APROVADA);

        List<RecursoDisponivelResponse> lista = elegiveis.listar(osId, etapaId);

        assertEquals(List.of(joao), lista.stream().map(RecursoDisponivelResponse::id).toList());
        assertEquals(1, lista.getFirst().precosDoPar().size(), "só o preço do par da etapa");
    }

    // ------------------------------------------------------------------ fixtures

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome + " " + marca);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    private UUID novoTipo(String nome) {
        TipoServico tipo = new TipoServico();
        tipo.setNome(nome + " " + marca);
        return tiposServico.saveAndFlush(tipo).getId();
    }

    /** Recurso verificado; cada par vira um preço por palavra. */
    private UUID criarRecurso(String nome, UUID usuarioId, List<UUID> tipos, List<UUID[]> pares) {
        UUID id = new TransactionTemplate(transactionManager).execute(status -> {
            Recurso recurso = new Recurso(nome + " " + marca, UUID.randomUUID() + "@teste.invalid", "11988887777");
            if (usuarioId != null) {
                recurso.vincularUsuario(usuarioId);
            }
            recurso.definirVerificacao(true);
            recurso.definirTiposServico(tiposServico.findAllById(tipos));
            recurso.definirPrecos(pares.stream().map(par -> new Recurso.NovoPreco(
                    idiomas.findById(par[0]).orElseThrow(), idiomas.findById(par[1]).orElseThrow(),
                    UnidadeCobranca.PALAVRA, new BigDecimal("0.16"))).toList());
            return recursos.saveAndFlush(recurso).getId();
        });
        recursosCriados.add(id);
        return id;
    }

    /** US-024: certificação do par, aprovada ou ainda em análise. */
    private void certificar(UUID recursoId, UUID origemId, UUID destinoId, StatusCertificacao status) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            RecursoCertificacao certificacao = new RecursoCertificacao(recursos.findById(recursoId).orElseThrow(),
                    idiomas.findById(origemId).orElseThrow(), idiomas.findById(destinoId).orElseThrow(),
                    "diploma.pdf", "teste/diploma.pdf", Instant.now());
            if (status == StatusCertificacao.APROVADA) {
                certificacao.aprovar("gestor@teste.invalid", Instant.now());
            }
            certificacoes.saveAndFlush(certificacao);
        });
    }

    /** OS com uma etapa do tipo informado, para uma solicitação origem → destino. */
    private UUID criarOsComEtapa(UUID origemId, UUID destinoId, UUID tipoEtapaId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.findById(clienteId).orElseThrow();
            Solicitacao solicitacao = solicitacoes.saveAndFlush(new Solicitacao(cliente,
                    categorias.findById(categoriaId).orElseThrow(),
                    idiomas.findById(origemId).orElseThrow(), idiomas.findById(destinoId).orElseThrow(),
                    "Solicitação " + marca));
            solicitacaoId = solicitacao.getId();
            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setSolicitacao(solicitacao);
            orcamento.setStatus(StatusOrcamento.APROVADO);
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();
            osId = ordensServico.saveAndFlush(new OrdemServico(orcamento)).getId();
        });
        return etapasService.definirManualmente(osId, new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Formatação", tipoEtapaId, 8))))
                .etapas().stream().map(EtapaOrdemServicoResponse::id).findFirst().orElseThrow();
    }
}
