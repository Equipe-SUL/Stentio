package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoCriacaoRequest;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Executar somente com um PostgreSQL de testes, como os demais testes de integração do Core.
@EnabledIfEnvironmentVariable(named = "ORCAMENTO_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${ORCAMENTO_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/stentio_test}",
        "spring.datasource.username=${ORCAMENTO_TEST_DB_USER:postgres}",
        "spring.datasource.password=${ORCAMENTO_TEST_DB_PASSWORD:postgres}",
        "spring.autoconfigure.exclude=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.orcamentos.outbox.enabled=false",
        "app.seed.test-data.enabled=false"
})
class OrcamentoCriacaoPostgresTest {
    @Autowired OrcamentoService service;
    @Autowired SolicitacaoRepository solicitacoes;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaProjetoRepository categorias;
    @Autowired IdiomaRepository idiomas;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private UUID clienteId;
    private UUID categoriaId;
    private UUID origemId;
    private UUID destinoId;
    private UUID solicitacaoId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Teste orçamento", "Cliente teste",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "52998224725"));
            clienteId = cliente.getId();
            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Teste orçamento " + UUID.randomUUID());
            categoria = categorias.saveAndFlush(categoria);
            categoriaId = categoria.getId();
            Idioma origem = novoIdioma("Português", "pt");
            Idioma destino = novoIdioma("Inglês", "en");
            origemId = origem.getId();
            destinoId = destino.getId();
            solicitacaoId = solicitacoes.saveAndFlush(new Solicitacao(
                    cliente, categoria, origem, destino, "Teste de criação concorrente")).getId();
        });
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from orcamentos where solicitacao_id = ?", solicitacaoId);
            jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            jdbc.update("delete from idiomas where id in (?, ?)", origemId, destinoId);
        });
    }

    @Test
    void duasCriacoesSimultaneasGeramUmUnicoOrcamento() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try {
            var primeira = executor.submit(() -> criarConcorrente(prontas, iniciar));
            var segunda = executor.submit(() -> criarConcorrente(prontas, iniciar));
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            int sucessos = (primeira.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(15, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, sucessos);
            assertEquals(1, contarOrcamentos());
            assertEquals("AGUARDANDO_APROVACAO", statusSolicitacao());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void falhaNaTransacaoNaoDeixaSolicitacaoBloqueada() {
        assertThrows(IllegalStateException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    service.criar(request());
                    throw new IllegalStateException("Falha simulada antes do commit");
                }));

        assertEquals(0, contarOrcamentos());
        assertEquals("PENDENTE_ORCAMENTO", statusSolicitacao());
        service.criar(request());
        assertEquals(1, contarOrcamentos());
        assertEquals("AGUARDANDO_APROVACAO", statusSolicitacao());
    }

    private boolean criarConcorrente(CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout no teste");
        try {
            service.criar(request());
            return true;
        } catch (OrcamentoInvalidoException | SolicitacaoInvalidaException ex) {
            return false;
        }
    }

    private OrcamentoCriacaoRequest request() {
        return new OrcamentoCriacaoRequest(solicitacaoId, clienteId, Instant.now().plusSeconds(3600));
    }

    private int contarOrcamentos() {
        return jdbc.queryForObject("select count(*) from orcamentos where solicitacao_id = ?",
                Integer.class, solicitacaoId);
    }

    private String statusSolicitacao() {
        return jdbc.queryForObject("select status from solicitacoes where id = ?", String.class, solicitacaoId);
    }

    private Idioma novoIdioma(String nome, String codigo) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(codigo + "-" + UUID.randomUUID().toString().substring(0, 6));
        return idiomas.saveAndFlush(idioma);
    }
}
