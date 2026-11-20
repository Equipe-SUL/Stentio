package com.stentio.core.service;

import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-014 T0 contra o Postgres real: a consulta da etapa anterior
 * ({@code EtapaOrdemServicoRepository.anteriorDe}) e o 409 do disparo fora de ordem, com a
 * etapa intacta no banco depois da recusa.
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
class OrdemDasEtapasPostgresTest {

    @Autowired DisparoEtapaService disparo;
    @Autowired EtapaOrdemServicoService etapasService;
    @Autowired EtapaOrdemServicoRepository etapas;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaProjetoRepository categorias;
    @Autowired IdiomaRepository idiomas;
    @Autowired SolicitacaoRepository solicitacoes;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired OrdemServicoRepository ordensServico;
    @Autowired TipoServicoRepository tiposServico;
    @Autowired RecursoRepository recursos;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private final String marca = "US014T0-" + UUID.randomUUID().toString().substring(0, 8);
    private final UsuarioAutenticado gestor = new UsuarioAutenticado("gestor.us014@teste.invalid", false);

    private UUID clienteId;
    private UUID categoriaId;
    private UUID portuguesId;
    private UUID inglesId;
    private UUID solicitacaoId;
    private UUID orcamentoId;
    private UUID osId;
    private UUID traducaoId;
    private UUID anaId;
    private UUID etapa1Id;
    private UUID etapa2Id;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa " + marca, "Cliente US014",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "86288366757"));
            clienteId = cliente.getId();

            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria " + marca);
            categoriaId = categorias.saveAndFlush(categoria).getId();

            Idioma portugues = novoIdioma("Português");
            Idioma ingles = novoIdioma("Inglês");
            portuguesId = portugues.getId();
            inglesId = ingles.getId();

            Solicitacao solicitacao = solicitacoes.saveAndFlush(
                    new Solicitacao(cliente, categoria, portugues, ingles, "Solicitação " + marca));
            solicitacaoId = solicitacao.getId();

            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setSolicitacao(solicitacao);
            orcamento.setStatus(StatusOrcamento.APROVADO);
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();
            osId = ordensServico.saveAndFlush(new OrdemServico(orcamento)).getId();

            TipoServico traducao = new TipoServico();
            traducao.setNome("Tradução " + marca);
            traducaoId = tiposServico.saveAndFlush(traducao).getId();

            Recurso ana = new Recurso("Ana " + marca, UUID.randomUUID() + "@teste.invalid", "11988887777");
            ana.definirVerificacao(true);
            ana.definirTiposServico(List.of(traducao));
            ana.definirPrecos(List.of(new Recurso.NovoPreco(
                    portugues, ingles, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
            anaId = recursos.saveAndFlush(ana).getId();
        });

        List<UUID> ids = etapasService.definirManualmente(osId, new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Tradução", traducaoId, 24),
                        new EtapasOrdemServicoRequest.Etapa(2, "Revisão", traducaoId, 12))))
                .etapas().stream().map(EtapaOrdemServicoResponse::id).toList();
        etapa1Id = ids.get(0);
        etapa2Id = ids.get(1);
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String etapasDaOs = "select id from etapas_os where os_id = ?";
            jdbc.update("delete from notificacao_etapa_outbox where etapa_id in (" + etapasDaOs + ")", osId);
            jdbc.update("delete from etapas_os_historico where etapa_id in (" + etapasDaOs + ")", osId);
            jdbc.update("delete from log_auditoria where entidade_id in (" + etapasDaOs + ") or entidade_id = ?",
                    osId, osId);
            jdbc.update("delete from etapas_os where os_id = ?", osId);
            jdbc.update("delete from ordens_servico where id = ?", osId);
            jdbc.update("delete from orcamentos where id = ?", orcamentoId);
            jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            jdbc.update("delete from recursos_precos where recurso_id = ?", anaId);
            jdbc.update("delete from recursos_tipos_servico where recurso_id = ?", anaId);
            jdbc.update("delete from recursos where id = ?", anaId);
            jdbc.update("delete from tipos_servico where id = ?", traducaoId);
            jdbc.update("delete from idiomas where id in (?, ?)", portuguesId, inglesId);
        });
    }

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    @Test
    @DisplayName("anteriorDe encontra a etapa de ordem − 1 da mesma OS, e nada para a primeira")
    void consultaDaAnterior() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            EtapaOrdemServico etapa1 = etapas.findById(etapa1Id).orElseThrow();
            EtapaOrdemServico etapa2 = etapas.findById(etapa2Id).orElseThrow();

            assertNull(etapas.anteriorDe(etapa1));
            assertEquals(etapa1Id, etapas.anteriorDe(etapa2).getId());
        });
    }

    @Test
    @DisplayName("Cenário 1: disparar a etapa 2 antes da etapa 1 responde 409 e nada muda no banco")
    void disparoForaDeOrdem() {
        assertThrows(TransicaoEtapaInvalidaException.class, () -> disparo.disparar(osId, etapa2Id, gestor));

        assertEquals("AGUARDANDO_ETAPA_ANTERIOR",
                jdbc.queryForObject("select status from etapas_os where id = ?", String.class, etapa2Id));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from notificacao_etapa_outbox where etapa_id = ?", Integer.class, etapa2Id));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from etapas_os_historico where etapa_id = ?", Integer.class, etapa2Id));
    }

    @Test
    @DisplayName("Depois que a etapa 1 é entregue, a etapa 2 pode ser disparada")
    void disparoDepoisDaEntrega() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            EtapaOrdemServico etapa1 = etapas.findById(etapa1Id).orElseThrow();
            Recurso ana = recursos.findById(anaId).orElseThrow();
            etapa1.disparar(null, Instant.now());
            etapa1.aceitarPor(null, ana, Instant.now());
            etapa1.entregar(null, "etapa1.docx", "2026/11/etapa1.docx", Instant.now());
            etapas.saveAndFlush(etapa1);
        });

        disparo.disparar(osId, etapa2Id, gestor);

        assertEquals("DISPARADA",
                jdbc.queryForObject("select status from etapas_os where id = ?", String.class, etapa2Id));
    }
}
