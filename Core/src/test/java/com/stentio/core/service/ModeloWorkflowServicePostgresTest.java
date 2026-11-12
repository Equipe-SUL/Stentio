package com.stentio.core.service;

import com.stentio.core.dto.ModeloWorkflowRequest;
import com.stentio.core.dto.ModeloWorkflowResponse;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.TipoServicoRepository;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regressão do PUT de modelo no Postgres real: substituir a sequência de etapas precisa
 * apagar as órfãs antes de inserir as novas, senão a unique (modelo_id, ordem) estoura 409.
 * Usa o mesmo opt-in e banco da US-14/US-16/US-17.
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
        "app.orcamentos.outbox.enabled=false"
})
class ModeloWorkflowServicePostgresTest {

    @Autowired
    private ModeloWorkflowService service;

    @Autowired
    private TipoServicoRepository tiposServico;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID traducaoId;
    private UUID revisaoId;
    private final List<UUID> modelosIds = new ArrayList<>();

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            traducaoId = novoTipoServico("Tradução teste").getId();
            revisaoId = novoTipoServico("Revisão teste").getId();
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (UUID modelo : modelosIds) {
                jdbc.update("delete from modelos_workflow_etapas where modelo_id = ?", modelo);
                jdbc.update("delete from modelos_workflow where id = ?", modelo);
            }
            jdbc.update("delete from tipos_servico where id in (?, ?)", traducaoId, revisaoId);
        });
        modelosIds.clear();
    }

    @Test
    @DisplayName("PUT: substitui mantendo as ordens 1 e 2 sem colidir com a unique (modelo_id, ordem)")
    void deveSubstituirMantendoAsMesmasOrdens() {
        ModeloWorkflowResponse criado = service.criar(novoModelo(
                etapa(1, "Tradução inicial", traducaoId, 20),
                etapa(2, "Revisão inicial", revisaoId, 8)));
        modelosIds.add(criado.id());

        ModeloWorkflowResponse atualizado = service.substituir(criado.id(), novoModelo(
                etapa(1, "Tradução v2", traducaoId, 22),
                etapa(2, "Revisão v2", revisaoId, 9)));

        List<ModeloWorkflowResponse.Etapa> etapas = porOrdem(atualizado);
        assertEquals(List.of(1, 2), etapas.stream().map(ModeloWorkflowResponse.Etapa::ordem).toList());
        assertEquals("Tradução v2", etapas.get(0).nome());
        assertEquals(22, etapas.get(0).prazoEstimadoHoras());
        assertEquals("Revisão v2", etapas.get(1).nome());
        assertEquals(9, etapas.get(1).prazoEstimadoHoras());
        assertEquals(2, contarEtapas(criado.id()));
    }

    @Test
    @DisplayName("PUT: substitui invertendo os tipos de serviço entre as ordens")
    void deveSubstituirInvertendoOsTiposServico() {
        ModeloWorkflowResponse criado = service.criar(novoModelo(
                etapa(1, "Tradução", traducaoId, 20),
                etapa(2, "Revisão", revisaoId, 8)));
        modelosIds.add(criado.id());

        ModeloWorkflowResponse atualizado = service.substituir(criado.id(), novoModelo(
                etapa(2, "Tradução", traducaoId, 20),
                etapa(1, "Revisão", revisaoId, 8)));

        List<ModeloWorkflowResponse.Etapa> etapas = porOrdem(atualizado);
        assertEquals(List.of(1, 2), etapas.stream().map(ModeloWorkflowResponse.Etapa::ordem).toList());
        assertEquals("Revisão", etapas.get(0).nome());
        assertEquals(revisaoId, etapas.get(0).tipoServicoId());
        assertEquals("Tradução", etapas.get(1).nome());
        assertEquals(traducaoId, etapas.get(1).tipoServicoId());
        assertEquals(2, contarEtapas(criado.id()));
    }

    private TipoServico novoTipoServico(String nome) {
        TipoServico tipo = new TipoServico();
        tipo.setNome(nome + " " + UUID.randomUUID());
        return tiposServico.saveAndFlush(tipo);
    }

    private ModeloWorkflowRequest novoModelo(ModeloWorkflowRequest.Etapa... etapas) {
        return new ModeloWorkflowRequest("Modelo " + UUID.randomUUID(), null, List.of(etapas));
    }

    private ModeloWorkflowRequest.Etapa etapa(int ordem, String nome, UUID tipoServicoId, int prazoHoras) {
        return new ModeloWorkflowRequest.Etapa(ordem, nome, tipoServicoId, prazoHoras);
    }

    private List<ModeloWorkflowResponse.Etapa> porOrdem(ModeloWorkflowResponse modelo) {
        return modelo.etapas().stream()
                .sorted(Comparator.comparing(ModeloWorkflowResponse.Etapa::ordem))
                .toList();
    }

    private int contarEtapas(UUID modeloId) {
        return jdbc.queryForObject(
                "select count(*) from modelos_workflow_etapas where modelo_id = ?", Integer.class, modeloId);
    }
}
