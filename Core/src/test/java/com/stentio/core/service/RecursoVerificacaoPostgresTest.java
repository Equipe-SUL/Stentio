package com.stentio.core.service;

import com.stentio.core.model.Recurso;
import com.stentio.core.repository.RecursoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US-023 CA.6 contra o Postgres real.
 *
 * <p>O teste de service com Mockito não alcança o defeito que importa aqui: um método
 * sob {@code readOnly} altera o objeto em memória, mas o Hibernate não faz flush. Só uma
 * leitura posterior no banco prova que a verificação foi realmente persistida.
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
class RecursoVerificacaoPostgresTest {

    @Autowired
    private RecursoService service;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID recursoId;

    @BeforeEach
    void preparar() {
        Recurso recurso = new Recurso(
                "Recurso US023",
                UUID.randomUUID() + "@teste.invalid",
                "+55 11 99999-0000");
        recursoId = recursos.saveAndFlush(recurso).getId();
    }

    @AfterEach
    void limpar() {
        if (recursoId != null) {
            jdbc.update("delete from recursos where id = ?", recursoId);
        }
    }

    @Test
    @DisplayName("CA.6: a verificação feita pelo gestor é persistida no banco")
    void persisteVerificacao() {
        service.alterarVerificacao(recursoId, true);

        assertTrue(jdbc.queryForObject(
                "select verificado from recursos where id = ?", Boolean.class, recursoId));

        service.alterarVerificacao(recursoId, false);

        assertFalse(jdbc.queryForObject(
                "select verificado from recursos where id = ?", Boolean.class, recursoId));
    }
}
