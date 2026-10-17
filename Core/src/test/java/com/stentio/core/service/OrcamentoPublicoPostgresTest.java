package com.stentio.core.service;

import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Explicitamente opt-in; exige PostgreSQL e cria dados apenas no banco de testes.
@EnabledIfEnvironmentVariable(named = "ORCAMENTO_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${ORCAMENTO_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/stentio_test}",
        "spring.datasource.username=${ORCAMENTO_TEST_DB_USER:postgres}",
        "spring.datasource.password=${ORCAMENTO_TEST_DB_PASSWORD:postgres}",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.autoconfigure.exclude=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "app.orcamentos.outbox.enabled=false"
})
class OrcamentoPublicoPostgresTest {
    @Autowired OrcamentoPublicoService service;
    @Autowired OrcamentoTokenService tokenService;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired ClienteRepository clientes;
    @Autowired IdiomaRepository idiomas;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean OrcamentoDecisaoOutboxRepository outbox;
    private UUID clienteId;
    private UUID idiomaOrigemId;
    private UUID idiomaDestinoId;
    private UUID orcamentoId;
    private String token;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa de Teste", "Cliente Teste",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "52998224725"));
            clienteId = cliente.getId();
            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setStatus(StatusOrcamento.AGUARDANDO_APROVACAO);
            orcamento.setValorTotal(new BigDecimal("1500.00"));
            OrcamentoItemServico servico = new OrcamentoItemServico();
            servico.setTipoServico("Tradução Juramentada");
            servico.setQuantidade(new BigDecimal("5000"));
            servico.setUnidade(UnidadeCobranca.PALAVRA);
            servico.setValorUnitario(new BigDecimal("0.30"));
            servico.setSubtotal(new BigDecimal("1500.00"));
            OrcamentoItem item = new OrcamentoItem();
            item.setOrdem(1);
            item.setIdiomaOrigem(novoIdioma("Português", "pt"));
            item.setIdiomaDestino(novoIdioma("Inglês", "en"));
            idiomaOrigemId = item.getIdiomaOrigem().getId();
            idiomaDestinoId = item.getIdiomaDestino().getId();
            item.setSubtotalItem(new BigDecimal("1500.00"));
            item.adicionarServico(servico);
            orcamento.adicionarItem(item);
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();
            token = tokenService.emitir(orcamentoId, Instant.now().plusSeconds(3600)).toString();
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from orcamento_decisao_outbox where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_historico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamento_item where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamentos where id = ?", orcamentoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from idiomas where id in (?, ?)", idiomaOrigemId, idiomaDestinoId);
        });
    }

    private Idioma novoIdioma(String nome, String codigoIso) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        // Código aleatório para não colidir com idiomas já cadastrados no banco de testes.
        idioma.setCodigoIso(codigoIso + "-" + UUID.randomUUID().toString().substring(0, 6));
        return idiomas.saveAndFlush(idioma);
    }

    @Test
    void aprovacaoERejeicaoSimultaneasGravamSomenteUmaDecisao() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Boolean> aprovacao = executor.submit(() -> decidirConcorrente(true, prontas, iniciar));
            Future<Boolean> rejeicao = executor.submit(() -> decidirConcorrente(false, prontas, iniciar));
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            int sucessos = (aprovacao.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (rejeicao.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, sucessos);
            assertEquals(1, contar("orcamento_historico"));
            assertEquals(1, contar("orcamento_decisao_outbox"));
            assertThrows(LinkOrcamentoInvalidoException.class, () -> service.consultar(token));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    private boolean decidirConcorrente(boolean aprovar, CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout no teste");
        try {
            if (aprovar) service.aprovar(token);
            else service.rejeitar(token, "Prazo insuficiente");
            return true;
        } catch (LinkOrcamentoInvalidoException ex) {
            return false;
        }
    }

    @Test
    void falhaAoGravarEventoDesfazStatusHistoricoEConsumoDoToken() {
        doThrow(new IllegalStateException("Falha simulada de persistência")).when(outbox).save(any());
        assertThrows(IllegalStateException.class, () -> service.aprovar(token));
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, service.consultar(token).status());
        assertEquals(0, contar("orcamento_historico"));
        assertEquals(0, contar("orcamento_decisao_outbox"));
        assertNull(jdbc.queryForObject("select token_utilizado_em from orcamentos where id = ?", Instant.class, orcamentoId));
    }

    @Test
    void rejeicaoPersisteMotivoEOutboxNaMesmaTransacao() {
        service.rejeitar(token, "Valor acima do limite");
        assertEquals("REJEITADO", jdbc.queryForObject("select status from orcamentos where id = ?", String.class, orcamentoId));
        assertEquals("Valor acima do limite", jdbc.queryForObject(
                "select motivo from orcamento_historico where orcamento_id = ?", String.class, orcamentoId));
        assertEquals(1, contar("orcamento_decisao_outbox"));
    }

    private int contar(String tabela) {
        // Nome da tabela definido exclusivamente pelo teste.
        return jdbc.queryForObject("select count(*) from " + tabela + " where orcamento_id = ?", Integer.class, orcamentoId);
    }
}
