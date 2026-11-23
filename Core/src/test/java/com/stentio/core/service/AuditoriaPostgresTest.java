package com.stentio.core.service;

import com.stentio.core.config.DadosUsuarioToken;
import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.dto.LogAuditoriaPaginaResponse;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import com.stentio.core.support.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-019 contra o Postgres real. O que os testes de mock não alcançam:
 *
 * <ul>
 *   <li>o {@code @TransactionalEventListener(BEFORE_COMMIT)} de verdade: o log só existe se a
 *       transação da mudança de status for confirmada;</li>
 *   <li>o SecurityContext da thread chegando ao listener, que tira dali o ator;</li>
 *   <li>as Specifications virando SQL — busca com CAST do UUID, intervalo de datas,
 *       paginação e ordenação.</li>
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
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoTemplateAutoConfiguration",
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.orcamentos.outbox.enabled=false",
        "app.seed.test-data.enabled=false"
})
class AuditoriaPostgresTest {

    private static final Instant DAQUI_A_UM_MES = Instant.now().plus(30, ChronoUnit.DAYS);
    private static final UUID GESTOR_ID = UUID.randomUUID();

    @Autowired AlocacaoEtapaService alocacao;
    @Autowired EtapaOrdemServicoService etapasService;
    @Autowired OrcamentoPublicoService publicoService;
    @Autowired OrcamentoTokenService tokenService;
    @Autowired OrdemServicoService ordemServicoService;
    @Autowired LogAuditoriaService logService;
    @Autowired LogAuditoriaRepository logs;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaProjetoRepository categorias;
    @Autowired IdiomaRepository idiomas;
    @Autowired SolicitacaoRepository solicitacoes;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired OrdemServicoRepository ordensServico;
    @Autowired TipoServicoRepository tiposServico;
    @Autowired RecursoRepository recursos;
    @Autowired RecursoCertificacaoRepository certificacoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    /** Marca única deste teste: isola as consultas dos logs que outros testes deixaram no banco. */
    private final String marca = "US019-" + UUID.randomUUID().toString().substring(0, 8);

    private UUID clienteId;
    private UUID categoriaId;
    private UUID portuguesId;
    private UUID inglesId;
    private UUID solicitacaoId;
    private UUID orcamentoId;
    private UUID osId;
    private UUID traducaoId;
    private UUID anaId;
    private UUID brunoId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa US019", "Cliente US019",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "39053344705"));
            clienteId = cliente.getId();

            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria " + marca);
            categoriaId = categorias.saveAndFlush(categoria).getId();

            Idioma portugues = novoIdioma("Português");
            Idioma ingles = novoIdioma("Inglês");
            portuguesId = portugues.getId();
            inglesId = ingles.getId();

            Solicitacao solicitacao = new Solicitacao(cliente, categoria, portugues, ingles, "Solicitação " + marca);
            solicitacao.iniciarOrcamento();
            solicitacaoId = solicitacoes.saveAndFlush(solicitacao).getId();

            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setSolicitacao(solicitacao);
            orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
            orcamento.setValorTotal(new BigDecimal("500.00"));
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();

            TipoServico traducao = new TipoServico();
            traducao.setNome("Tradução " + marca);
            traducaoId = tiposServico.saveAndFlush(traducao).getId();

            anaId = novoRecurso("Ana Silva", traducao, portugues, ingles);
            brunoId = novoRecurso("Bruno Lima", traducao, portugues, ingles);
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // O log é append-only para a aplicação; aqui é só a faxina do banco de teste.
            jdbc.update("delete from log_auditoria where referencia_legivel like ? or entidade_id in "
                            + "(select id from etapas_os where os_id in (select id from ordens_servico where orcamento_id = ?)) "
                            + "or entidade_id in (?, (select id from ordens_servico where orcamento_id = ?))",
                    "%" + marca + "%", orcamentoId, orcamentoId, orcamentoId);
            jdbc.update("delete from etapas_os_historico where etapa_id in (select id from etapas_os where os_id in "
                    + "(select id from ordens_servico where orcamento_id = ?))", orcamentoId);
            jdbc.update("delete from etapas_os where os_id in (select id from ordens_servico where orcamento_id = ?)", orcamentoId);
            jdbc.update("delete from ordens_servico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_decisao_outbox where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_historico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamentos where id = ?", orcamentoId);
            jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            for (UUID recurso : List.of(anaId, brunoId)) {
                jdbc.update("delete from recursos_certificacoes where recurso_id = ?", recurso);
                jdbc.update("delete from recursos_precos where recurso_id = ?", recurso);
                jdbc.update("delete from recursos_tipos_servico where recurso_id = ?", recurso);
                jdbc.update("delete from recursos where id = ?", recurso);
            }
            jdbc.update("delete from tipos_servico where id = ?", traducaoId);
            jdbc.update("delete from idiomas where id in (?, ?)", portuguesId, inglesId);
        });
    }

    // ------------------------------------------------------------------ cenários

    @Test
    @DisplayName("Cenário 1: gestor aloca a etapa e o Administrador encontra o registro filtrando por ORDEM_SERVICO_ETAPA")
    void alocacaoGeraLogConsultavel() {
        criarOrdemServicoAprovandoOrcamento();
        autenticarGestor();
        UUID etapaId = montarEtapas().getFirst();

        Instant antes = Instant.now();
        alocacao.alocar(osId, etapaId, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestorAutenticado());

        LogAuditoriaPaginaResponse pagina = logService.listar(new LogAuditoriaFiltro(
                EntidadeAuditada.ORDEM_SERVICO_ETAPA, etapaId, null, null, null, null), 1, 20);

        // Criação (null -> PENDENTE) e alocação (PENDENTE -> ALOCADO), mais recente primeiro.
        assertEquals(2, pagina.totalRegistros());
        LogAuditoriaPaginaResponse.Log alocado = pagina.logs().getFirst();
        assertEquals("PENDENTE", alocado.statusAnterior());
        assertEquals("ALOCADO", alocado.statusNovo());
        assertEquals("Carlos Mendes", alocado.usuario().nome());
        assertEquals("GESTOR_PROJETO", alocado.usuario().perfil());
        assertEquals(GESTOR_ID, alocado.usuario().id());
        assertEquals(OrigemAcao.PAINEL_WEB, alocado.origemAcao());
        assertEquals("Recurso Ana Silva alocado à etapa.", alocado.observacao());
        assertTrue(alocado.referenciaLegivel().endsWith("- Etapa 1 (Tradução)"), alocado.referenciaLegivel());
        assertFalse(alocado.dataHora().isBefore(antes.truncatedTo(ChronoUnit.MILLIS)));

        LogAuditoriaPaginaResponse.Log criada = pagina.logs().get(1);
        assertNull(criada.statusAnterior());
        assertEquals("PENDENTE", criada.statusNovo());
    }

    @Test
    @DisplayName("Realocar não muda o status: o histórico da etapa registra, a auditoria não duplica")
    void realocacaoNaoGeraNovoLog() {
        criarOrdemServicoAprovandoOrcamento();
        autenticarGestor();
        UUID etapaId = montarEtapas().getFirst();

        alocacao.alocar(osId, etapaId, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestorAutenticado());
        alocacao.alocar(osId, etapaId, new AlocacaoEtapaRequest(brunoId, DAQUI_A_UM_MES), gestorAutenticado());

        assertEquals(2, contarLogs(etapaId), "criação + primeira alocação");
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from etapas_os_historico where etapa_id = ?", Integer.class, etapaId));
    }

    @Test
    @DisplayName("CA.4: aprovação pelo link público gera log com origem TOKEN_PUBLICO e a OS nasce pelo SISTEMA")
    void aprovacaoPorLinkPublico() {
        String token = new TransactionTemplate(transactionManager).execute(status ->
                tokenService.emitir(orcamentoId, Instant.now().plusSeconds(3600)).toString());

        publicoService.aprovar(token);

        LogAuditoriaPaginaResponse.Log aprovado = logService.listar(new LogAuditoriaFiltro(
                EntidadeAuditada.ORCAMENTO, orcamentoId, null, null, null, null), 1, 20).logs().getFirst();
        assertEquals("AGUARDANDO_APROVACAO", aprovado.statusAnterior());
        assertEquals("APROVADO", aprovado.statusNovo());
        assertEquals(OrigemAcao.TOKEN_PUBLICO, aprovado.origemAcao());
        assertEquals("Cliente (Link Público)", aprovado.usuario().nome());
        assertEquals("CLIENTE_EXTERNO", aprovado.usuario().perfil());
        assertNull(aprovado.usuario().id());
        assertEquals("Aprovado via token de e-mail.", aprovado.observacao());

        // O consumidor da fila cria a OS sem ninguém logado.
        UUID os = ordemServicoService.criarParaOrcamentoAprovado(orcamentoId).getId();
        LogAuditoriaPaginaResponse.Log criada = logService.listar(new LogAuditoriaFiltro(
                EntidadeAuditada.ORDEM_SERVICO, os, null, null, null, null), 1, 20).logs().getFirst();
        assertEquals(OrigemAcao.SISTEMA, criada.origemAcao());
        assertNull(criada.statusAnterior());
        assertEquals("PENDENTE_ALOCACAO", criada.statusNovo());
    }

    @Test
    @DisplayName("CA.1: transação desfeita não deixa log — a auditoria acompanha a mudança de status")
    void rollbackNaoDeixaLog() {
        criarOrdemServicoAprovandoOrcamento();
        autenticarGestor();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            montarEtapas();
            status.setRollbackOnly();
        });

        assertEquals(0, contarLogs(osId));
        assertEquals(0, jdbc.queryForObject("select count(*) from etapas_os where os_id = ?", Integer.class, osId));
    }

    @Test
    @DisplayName("CA.3: filtros combinados, intervalo de datas inclusivo e paginação")
    void filtrosCombinadosEPaginacao() {
        UUID outroUsuario = UUID.randomUUID();
        Instant base = Instant.parse("2026-01-10T12:00:00Z");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (int i = 0; i < 5; i++) {
                gravar(EntidadeAuditada.ORCAMENTO, GESTOR_ID, base.plus(i, ChronoUnit.DAYS));
            }
            gravar(EntidadeAuditada.ORDEM_SERVICO, GESTOR_ID, base);
            gravar(EntidadeAuditada.ORCAMENTO, outroUsuario, base);
        });

        // Só a marca: os 7 registros deste teste.
        assertEquals(7, consultar(null, null, null, null, 1, 20).totalRegistros());

        // Entidade + usuário.
        assertEquals(5, consultar(EntidadeAuditada.ORCAMENTO, GESTOR_ID, null, null, 1, 20).totalRegistros());

        // Intervalo inclusivo nas duas pontas: dias 11, 12 e 13.
        assertEquals(3, consultar(EntidadeAuditada.ORCAMENTO, GESTOR_ID,
                base.plus(1, ChronoUnit.DAYS), base.plus(3, ChronoUnit.DAYS), 1, 20).totalRegistros());

        // Paginação: 5 registros em páginas de 2, do mais recente para o mais antigo.
        LogAuditoriaPaginaResponse primeira = consultar(EntidadeAuditada.ORCAMENTO, GESTOR_ID, null, null, 1, 2);
        LogAuditoriaPaginaResponse ultima = consultar(EntidadeAuditada.ORCAMENTO, GESTOR_ID, null, null, 3, 2);
        assertEquals(5, primeira.totalRegistros());
        assertEquals(3, primeira.totalPaginas());
        assertEquals(2, primeira.logs().size());
        assertEquals(base.plus(4, ChronoUnit.DAYS), primeira.logs().getFirst().dataHora());
        assertEquals(1, ultima.logs().size());
        assertEquals(base, ultima.logs().getFirst().dataHora());

        // A busca não diferencia maiúsculas.
        assertEquals(7, logService.listar(new LogAuditoriaFiltro(null, null, null, null, null,
                marca.toLowerCase()), 1, 20).totalRegistros());
    }

    @Test
    @DisplayName("Busca pelo trecho do id da entidade (CAST do UUID no SQL)")
    void buscaPorTrechoDoId() {
        UUID entidadeId = UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                logs.save(new LogAuditoria(EntidadeAuditada.ORCAMENTO, entidadeId, "Orçamento " + marca,
                        "EM_ELABORACAO", "AGUARDANDO_APROVACAO", GESTOR_ID, "Carlos Mendes", "GESTOR_PROJETO",
                        OrigemAcao.PAINEL_WEB, null, Instant.now())));

        String trecho = entidadeId.toString().substring(0, 8).toUpperCase();
        assertEquals(1, logService.listar(new LogAuditoriaFiltro(null, null, null, null, null, trecho), 1, 20)
                .totalRegistros());
    }

    @Test
    @DisplayName("Sem CHECK nas colunas de enum: um valor novo de origem não pode derrubar a transação")
    void tabelaSemCheckDeEnum() {
        Integer checks = jdbc.queryForObject("select count(*) from pg_constraint "
                + "where conrelid = 'log_auditoria'::regclass and contype = 'c'", Integer.class);
        assertEquals(0, checks);
    }

    // ------------------------------------------------------------------ fixtures

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    private UUID novoRecurso(String nome, TipoServico tipo, Idioma origem, Idioma destino) {
        Recurso recurso = new Recurso(nome, UUID.randomUUID() + "@teste.invalid", "11988887777");
        recurso.definirVerificacao(true);
        recurso.definirTiposServico(List.of(tipo));
        recurso.definirPrecos(List.of(new Recurso.NovoPreco(
                origem, destino, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
        Recurso salvo = recursos.saveAndFlush(recurso);
        certificar(salvo, origem, destino);
        return salvo.getId();
    }

    /** OS criada direto no banco, sem passar pela auditoria: o teste olha só o que vem depois. */
    private void criarOrdemServicoAprovandoOrcamento() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Orcamento orcamento = orcamentos.findById(orcamentoId).orElseThrow();
            orcamento.setStatus(StatusOrcamento.APROVADO);
            osId = ordensServico.saveAndFlush(new OrdemServico(orcamentos.saveAndFlush(orcamento))).getId();
        });
    }

    /** O que o JwtAuthFilter faz com um token do Auth: e-mail no principal, id e nome em details. */
    private static void autenticarGestor() {
        var autenticacao = new UsernamePasswordAuthenticationToken("carlos@stentio.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_GESTOR_PROJETO")));
        autenticacao.setDetails(new DadosUsuarioToken(GESTOR_ID, "Carlos Mendes"));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private static UsuarioAutenticado gestorAutenticado() {
        return UsuarioAutenticado.de(SecurityContextHolder.getContext().getAuthentication());
    }

    private List<UUID> montarEtapas() {
        return etapasService.definirManualmente(osId, new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Tradução", traducaoId, 24),
                        new EtapasOrdemServicoRequest.Etapa(2, "Revisão", traducaoId, 12))))
                .etapas().stream().map(EtapaOrdemServicoResponse::id).toList();
    }

    private int contarLogs(UUID entidadeId) {
        return jdbc.queryForObject("select count(*) from log_auditoria where entidade_id = ?", Integer.class, entidadeId);
    }

    private void gravar(EntidadeAuditada entidade, UUID usuarioId, Instant quando) {
        logs.save(new LogAuditoria(entidade, UUID.randomUUID(), "Registro " + marca, "A", "B",
                usuarioId, "Usuário", "ADMIN", OrigemAcao.PAINEL_WEB, null, quando));
    }

    private LogAuditoriaPaginaResponse consultar(EntidadeAuditada entidade, UUID usuarioId,
                                                 Instant inicio, Instant fim, int pagina, int tamanho) {
        return logService.listar(new LogAuditoriaFiltro(entidade, null, usuarioId, inicio, fim, marca), pagina, tamanho);
    }

    /**
     * US-024: o que decide elegibilidade agora é a certificação aprovada do par, não o
     * verificado. Sem esta linha o recurso existe, atende o par no cadastro e mesmo assim
     * não pode ser alocado.
     */
    private void certificar(Recurso recurso, Idioma origem, Idioma destino) {
        RecursoCertificacao certificacao = new RecursoCertificacao(
                recurso, origem, destino, "diploma.pdf", "2026/11/diploma.pdf", Instant.now());
        certificacao.aprovar("gestor@stentio.com", Instant.now());
        certificacoes.saveAndFlush(certificacao);
    }
}
