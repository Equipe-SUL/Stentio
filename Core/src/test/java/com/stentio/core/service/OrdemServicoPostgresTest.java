package com.stentio.core.service;

import com.stentio.core.consumer.OrcamentoAprovadoConsumer;
import com.stentio.core.event.OrcamentoDecisaoEvento;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

// Mesmo opt-in e banco de testes da US-14; o listener é chamado sem exigir um broker externo.
@EnabledIfEnvironmentVariable(named = "ORCAMENTO_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${ORCAMENTO_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/stentio_test}",
        "spring.datasource.username=${ORCAMENTO_TEST_DB_USER:postgres}",
        "spring.datasource.password=${ORCAMENTO_TEST_DB_PASSWORD:postgres}",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.autoconfigure.exclude=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.orcamentos.outbox.enabled=false"
})
class OrdemServicoPostgresTest {
    @Autowired OrdemServicoService service;
    @Autowired OrcamentoAprovadoConsumer consumer;
    @Autowired OrcamentoPublicoService aprovacao;
    @Autowired OrcamentoTokenService tokens;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired OrdemServicoRepository ordensServico;
    @Autowired SolicitacaoRepository solicitacoes;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaProjetoRepository categorias;
    @Autowired IdiomaRepository idiomas;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlatformTransactionManager transactionManager;
    private UUID clienteId;
    private UUID categoriaId;
    private UUID origemId;
    private UUID destinoId;
    private UUID solicitacaoId;
    private UUID orcamentoId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa US16", "Cliente US16",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "11144477735"));
            clienteId = cliente.getId();
            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria US16 " + UUID.randomUUID());
            categoriaId = categorias.saveAndFlush(categoria).getId();
            Idioma origem = novoIdioma("Português");
            Idioma destino = novoIdioma("Inglês");
            origemId = origem.getId();
            destinoId = destino.getId();
            Solicitacao solicitacao = solicitacoes.saveAndFlush(new Solicitacao(
                    cliente, categoria, origem, destino, "Solicitação US16"));
            solicitacaoId = solicitacao.getId();
            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setSolicitacao(solicitacao);
            orcamento.setStatus(StatusOrcamento.APROVADO);
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from ordens_servico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_decisao_outbox where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_historico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamentos where id = ?", orcamentoId);
            jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            jdbc.update("delete from idiomas where id in (?, ?)", origemId, destinoId);
        });
    }

    @Test
    void eventoRealDaAprovacaoCriaUmaOrdemMesmoQuandoReprocessado() {
        jdbc.update("update orcamentos set status = 'AGUARDANDO_APROVACAO' where id = ?", orcamentoId);
        String token = tokens.emitir(orcamentoId, Instant.now().plusSeconds(3600)).toString();
        aprovacao.aprovar(token);
        String payload = jdbc.queryForObject(
                "select payload from orcamento_decisao_outbox where orcamento_id = ?", String.class, orcamentoId);
        OrcamentoDecisaoEvento evento = objectMapper.readValue(payload, OrcamentoDecisaoEvento.class);

        consumer.consumir(evento);
        UUID primeiraOrdemId = ordensServico.findByOrcamentoId(orcamentoId).orElseThrow().getId();
        consumer.consumir(evento);

        assertEquals(primeiraOrdemId, ordensServico.findByOrcamentoId(orcamentoId).orElseThrow().getId());
        validarOrdemPersistida();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrcamento.class, names = "APROVADO", mode = EnumSource.Mode.EXCLUDE)
    void estadoAtualDoBancoImpedeCriacaoMesmoComEventoAprovado(StatusOrcamento status) {
        jdbc.update("update orcamentos set status = ? where id = ?", status.name(), orcamentoId);
        assertThrows(OrcamentoInvalidoException.class, () -> service.criarParaOrcamentoAprovado(orcamentoId));
        assertEquals(0, contarOrdens());
    }

    @Test
    void doisEventosConcorrentesTerminamComSucessoEUmaUnicaOrdem() throws Exception {
        OrcamentoDecisaoEvento evento = new OrcamentoDecisaoEvento(
                UUID.randomUUID(), 1, orcamentoId, StatusOrcamento.APROVADO, null, Instant.now());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Callable<Void> processar = () -> {
            prontas.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout no teste");
            consumer.consumir(evento);
            return null;
        };
        try {
            Future<Void> primeira = executor.submit(processar);
            Future<Void> segunda = executor.submit(processar);
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            primeira.get(15, TimeUnit.SECONDS);
            segunda.get(15, TimeUnit.SECONDS);
            validarOrdemPersistida();
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void restricaoUniqueProtegeBancoMesmoForaDoService() {
        service.criarParaOrcamentoAprovado(orcamentoId);
        assertThrows(DataIntegrityViolationException.class,
                () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Orcamento orcamento = orcamentos.findById(orcamentoId).orElseThrow();
                    ordensServico.saveAndFlush(new OrdemServico(orcamento));
                }));
        validarOrdemPersistida();
    }

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    private int contarOrdens() {
        return jdbc.queryForObject("select count(*) from ordens_servico where orcamento_id = ?", Integer.class, orcamentoId);
    }

    private void validarOrdemPersistida() {
        assertEquals(1, contarOrdens());
        var registro = jdbc.queryForMap("select * from ordens_servico where orcamento_id = ?", orcamentoId);
        assertNotNull(registro.get("id"));
        assertEquals(orcamentoId, registro.get("orcamento_id"));
        assertEquals(solicitacaoId, registro.get("solicitacao_id"));
        assertEquals("PENDENTE_ALOCACAO", registro.get("status"));
        assertNotNull(registro.get("criado_em"));
    }
}
