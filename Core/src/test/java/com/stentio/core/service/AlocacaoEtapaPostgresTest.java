package com.stentio.core.service;

import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.DashboardEtapaPendenciaProjection;
import com.stentio.core.dto.DashboardOrcamentoPendenciaProjection;
import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapasBloqueadasException;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-012 contra o Postgres real. O que os testes de mock não alcançam:
 *
 * <ul>
 *   <li>a listagem com {@code left join fetch e.recurso} — com open-in-view desligado, ler o
 *       nome do recurso fora da transação explodiria;</li>
 *   <li>o insert em {@code etapas_os_historico}, com duas chaves estrangeiras para
 *       {@code recursos} e a coluna de evento sob CHECK;</li>
 *   <li>a RN.3 da US-011, que só é verdade quando existe linha com {@code recurso_id}
 *       preenchido — antes desta US, nada preenchia essa coluna.</li>
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
        "app.orcamentos.outbox.enabled=false"
})
class AlocacaoEtapaPostgresTest {

    private static final Instant DAQUI_A_UM_MES = Instant.now().plusSeconds(30L * 24 * 3600);

    @Autowired AlocacaoEtapaService alocacao;
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

    private final UsuarioAutenticado gestor = new UsuarioAutenticado("gestor.us012@teste.invalid", false);

    private UUID clienteId;
    private UUID categoriaId;
    private UUID portuguesId;
    private UUID inglesId;
    private UUID alemaoId;
    private UUID solicitacaoId;
    private UUID orcamentoId;
    private UUID osId;
    private UUID traducaoId;
    private UUID revisaoId;
    private UUID anaId;
    private UUID brunoId;
    private UUID carlosId;
    private UUID danielaId;

    /** Solicitação Português → Inglês, com uma OS pronta para receber etapas. */
    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente cliente = clientes.saveAndFlush(new Cliente("Empresa US012", "Cliente US012",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", "11144477735"));
            clienteId = cliente.getId();

            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria US012 " + UUID.randomUUID());
            categoriaId = categorias.saveAndFlush(categoria).getId();

            Idioma portugues = novoIdioma("Português");
            Idioma ingles = novoIdioma("Inglês");
            Idioma alemao = novoIdioma("Alemão");
            portuguesId = portugues.getId();
            inglesId = ingles.getId();
            alemaoId = alemao.getId();

            Solicitacao solicitacao = solicitacoes.saveAndFlush(new Solicitacao(
                    cliente, categoria, portugues, ingles, "Solicitação US012"));
            solicitacaoId = solicitacao.getId();

            Orcamento orcamento = new Orcamento();
            orcamento.setCliente(cliente);
            orcamento.setSolicitacao(solicitacao);
            orcamento.setStatus(StatusOrcamento.APROVADO);
            orcamentoId = orcamentos.saveAndFlush(orcamento).getId();

            osId = ordensServico.saveAndFlush(new OrdemServico(orcamento)).getId();

            TipoServico traducao = novoTipoServico("Tradução US012");
            TipoServico revisao = novoTipoServico("Revisão US012");
            traducaoId = traducao.getId();
            revisaoId = revisao.getId();

            anaId = novoRecurso("Ana US012", traducao, portugues, ingles);
            brunoId = novoRecurso("Bruno US012", traducao, portugues, ingles);
            // Traduz, mas para outro par: deve bater na RN.1.
            carlosId = novoRecurso("Carlos US012", traducao, portugues, alemao);
            // Tem o par, mas só revisa: deve bater na RN.2.
            danielaId = novoRecurso("Daniela US012", revisao, portugues, ingles);
        });
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from etapas_os_historico where etapa_id in "
                    + "(select id from etapas_os where os_id = ?)", osId);
            jdbc.update("delete from etapas_os where os_id = ?", osId);
            jdbc.update("delete from ordens_servico where id = ?", osId);
            jdbc.update("delete from orcamento_historico where orcamento_id = ?", orcamentoId);
            jdbc.update("delete from orcamentos where id = ?", orcamentoId);
            jdbc.update("delete from solicitacoes where id = ?", solicitacaoId);
            jdbc.update("delete from clientes where id = ?", clienteId);
            jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            for (UUID recurso : List.of(anaId, brunoId, carlosId, danielaId)) {
                jdbc.update("delete from recursos_precos where recurso_id = ?", recurso);
                jdbc.update("delete from recursos_tipos_servico where recurso_id = ?", recurso);
                jdbc.update("delete from recursos where id = ?", recurso);
            }
            jdbc.update("delete from tipos_servico where id in (?, ?)", traducaoId, revisaoId);
            jdbc.update("delete from idiomas where id in (?, ?, ?)", portuguesId, inglesId, alemaoId);
        });
    }

    // ------------------------------------------------------------------ fixtures

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    private TipoServico novoTipoServico(String nome) {
        TipoServico tipo = new TipoServico();
        tipo.setNome(nome + " " + UUID.randomUUID());
        return tiposServico.saveAndFlush(tipo);
    }

    private UUID novoRecurso(String nome, TipoServico tipo, Idioma origem, Idioma destino) {
        Recurso recurso = new Recurso(nome, UUID.randomUUID() + "@teste.invalid", "11988887777");
        recurso.definirTiposServico(List.of(tipo));
        recurso.definirPrecos(List.of(new Recurso.NovoPreco(
                origem, destino, UnidadeCobranca.PALAVRA, new BigDecimal("0.35"))));
        return recursos.saveAndFlush(recurso).getId();
    }

    /** Monta três etapas na OS e devolve o id de cada uma, por ordem. */
    private List<UUID> montarEtapas() {
        EtapasOrdemServicoResponse resposta = etapasService.definirManualmente(osId,
                new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Tradução", traducaoId, 24),
                        new EtapasOrdemServicoRequest.Etapa(2, "Revisão", revisaoId, 12),
                        new EtapasOrdemServicoRequest.Etapa(3, "Entrega", traducaoId, 4))));
        return resposta.etapas().stream().map(EtapaOrdemServicoResponse::id).toList();
    }

    private int contarHistorico(UUID etapaId) {
        return jdbc.queryForObject(
                "select count(*) from etapas_os_historico where etapa_id = ?", Integer.class, etapaId);
    }

    // ------------------------------------------------------------------ testes

    @Test
    @DisplayName("A contagem inclui somente etapas alocadas com prazo estritamente vencido")
    void contaEtapasAtrasadasAlocadasSemCarregarEntidades() {
        Instant agora = Instant.parse("2030-01-01T00:00:00Z");
        List<EtapaOrdemServicoResponse> etapasDaOrdem = etapasService.definirManualmente(osId,
                new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Tradução", traducaoId, 24),
                        new EtapasOrdemServicoRequest.Etapa(2, "Revisão", revisaoId, 12),
                        new EtapasOrdemServicoRequest.Etapa(3, "Entrega", traducaoId, 4),
                        new EtapasOrdemServicoRequest.Etapa(4, "Conferência", revisaoId, 2))))
                .etapas();
        UUID vencida = etapasDaOrdem.get(0).id();
        UUID noLimite = etapasDaOrdem.get(1).id();
        UUID futura = etapasDaOrdem.get(2).id();
        UUID naoAlocada = etapasDaOrdem.get(3).id();

        alocacao.alocar(osId, vencida, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);
        alocacao.alocar(osId, noLimite, new AlocacaoEtapaRequest(danielaId, DAQUI_A_UM_MES), gestor);
        alocacao.alocar(osId, futura, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);

        jdbc.update("update etapas_os set data_limite_entrega = ? where id = ?",
                Timestamp.from(agora.minusSeconds(1)), vencida);
        jdbc.update("update etapas_os set data_limite_entrega = ? where id = ?",
                Timestamp.from(agora), noLimite);
        jdbc.update("update etapas_os set data_limite_entrega = ? where id = ?",
                Timestamp.from(agora.plusSeconds(1)), futura);
        jdbc.update("update etapas_os set data_limite_entrega = ? where id = ?",
                Timestamp.from(agora.minusSeconds(1)), naoAlocada);

        assertEquals(1, etapas.countByDataLimiteEntregaBeforeAndRecursoIsNotNull(agora));
    }

    @Test
    @DisplayName("As projeções de pendência consultam apenas os campos necessários")
    void buscaProjecoesDePendenciasDeEtapaEOrcamento() {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UUID etapaId = montarEtapas().getFirst();
        alocacao.alocar(osId, etapaId, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);
        jdbc.update("update etapas_os set data_limite_entrega = ? where id = ?",
                Timestamp.from(agora.minusSeconds(1)), etapaId);
        jdbc.update("update orcamentos set status = ?, data_expiracao = ? where id = ?",
                StatusOrcamento.AGUARDANDO_APROVACAO.name(),
                Timestamp.from(agora.plusSeconds(3600)),
                orcamentoId);

        DashboardEtapaPendenciaProjection etapa = etapas.buscarPendenciasDashboard(agora).stream()
                .filter(pendencia -> pendencia.id().equals(etapaId))
                .findFirst()
                .orElseThrow();
        DashboardOrcamentoPendenciaProjection orcamento = orcamentos.buscarPendenciasDashboard(List.of(
                        StatusOrcamento.EM_ELABORACAO, StatusOrcamento.AGUARDANDO_APROVACAO))
                .stream()
                .filter(pendencia -> pendencia.id().equals(orcamentoId))
                .findFirst()
                .orElseThrow();

        assertEquals("Tradução", etapa.nome());
        assertEquals(osId, etapa.ordemServicoId());
        assertEquals(agora.minusSeconds(1), etapa.dataLimiteEntrega());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, orcamento.status());
        assertEquals("Cliente US012", orcamento.nomeEmpresa());
        assertEquals(agora.plusSeconds(3600), orcamento.dataExpiracao());
    }

    @Test
    @DisplayName("Cenário 1 no banco: aloca, grava a data limite e deixa rastro no histórico")
    void alocaEGravaHistorico() {
        UUID primeira = montarEtapas().getFirst();

        alocacao.alocar(osId, primeira, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);

        Map<String, Object> etapa = jdbc.queryForMap(
                "select recurso_id, data_limite_entrega, alocado_em, status from etapas_os where id = ?", primeira);
        assertEquals(anaId, etapa.get("recurso_id"));
        assertNotNull(etapa.get("data_limite_entrega"));
        assertNotNull(etapa.get("alocado_em"));
        // A decisão central da US: o status gravado continua sendo o do fluxo.
        assertEquals("PENDENTE", etapa.get("status"));

        Map<String, Object> registro = jdbc.queryForMap(
                "select evento, recurso_anterior_id, recurso_novo_id, realizado_por "
                        + "from etapas_os_historico where etapa_id = ?", primeira);
        assertEquals("ALOCACAO", registro.get("evento"));
        assertNull(registro.get("recurso_anterior_id"));
        assertEquals(anaId, registro.get("recurso_novo_id"));
        assertEquals("gestor.us012@teste.invalid", registro.get("realizado_por"));
    }

    @Test
    @DisplayName("A listagem traz o nome do recurso fora da transação, sem lazy init")
    void listagemCarregaORecursoJunto() {
        UUID primeira = montarEtapas().getFirst();
        alocacao.alocar(osId, primeira, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);

        EtapasOrdemServicoResponse listagem = etapasService.listar(osId);

        EtapaOrdemServicoResponse alocada = listagem.etapas().getFirst();
        assertEquals(SituacaoEtapa.ALOCADO, alocada.status());
        assertEquals("Ana US012", alocada.recursoNome());
        assertEquals(anaId, alocada.recursoId());
        // As outras continuam sem recurso, e o left join não as elimina da listagem.
        assertEquals(3, listagem.etapas().size());
        assertNull(listagem.etapas().get(1).recursoNome());
        assertEquals(SituacaoEtapa.AGUARDANDO_ETAPA_ANTERIOR, listagem.etapas().get(1).status());
    }

    @Test
    @DisplayName("Realocar troca o recurso na etapa e acrescenta um evento, sem apagar o anterior")
    void realocarAcrescentaEventoSemApagarOAnterior() {
        UUID primeira = montarEtapas().getFirst();

        alocacao.alocar(osId, primeira, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);
        alocacao.alocar(osId, primeira, new AlocacaoEtapaRequest(brunoId, DAQUI_A_UM_MES), gestor);

        assertEquals(brunoId, jdbc.queryForObject(
                "select recurso_id from etapas_os where id = ?", UUID.class, primeira));
        assertEquals(2, contarHistorico(primeira));

        Map<String, Object> troca = jdbc.queryForMap(
                "select evento, recurso_anterior_id, recurso_novo_id from etapas_os_historico "
                        + "where etapa_id = ? and evento = 'REALOCACAO'", primeira);
        assertEquals(anaId, troca.get("recurso_anterior_id"));
        assertEquals(brunoId, troca.get("recurso_novo_id"));
    }

    @Test
    @DisplayName("Cenário 2 no banco: recurso sem o par é recusado e nada é gravado")
    void rn1NaoGravaNada() {
        UUID primeira = montarEtapas().getFirst();

        assertThrows(AlocacaoInvalidaException.class, () -> alocacao.alocar(
                osId, primeira, new AlocacaoEtapaRequest(carlosId, DAQUI_A_UM_MES), gestor));

        assertNull(jdbc.queryForObject("select recurso_id from etapas_os where id = ?", UUID.class, primeira));
        assertEquals(0, contarHistorico(primeira));
    }

    @Test
    @DisplayName("RN.2 no banco: recurso sem o tipo de serviço da etapa é recusado")
    void rn2NaoGravaNada() {
        UUID primeira = montarEtapas().getFirst();

        assertThrows(AlocacaoInvalidaException.class, () -> alocacao.alocar(
                osId, primeira, new AlocacaoEtapaRequest(danielaId, DAQUI_A_UM_MES), gestor));

        assertNull(jdbc.queryForObject("select recurso_id from etapas_os where id = ?", UUID.class, primeira));
        assertEquals(0, contarHistorico(primeira));
    }

    @Test
    @DisplayName("RN.3 da US-011 só vira verdade agora: sequência com etapa alocada não é remontada")
    void sequenciaAlocadaNaoPodeSerRemontada() {
        UUID primeira = montarEtapas().getFirst();
        alocacao.alocar(osId, primeira, new AlocacaoEtapaRequest(anaId, DAQUI_A_UM_MES), gestor);

        assertThrows(EtapasBloqueadasException.class, () -> etapasService.definirManualmente(osId,
                new EtapasOrdemServicoRequest(List.of(
                        new EtapasOrdemServicoRequest.Etapa(1, "Outra", traducaoId, 10)))));

        // A sequência original continua inteira, e o alocado segue no lugar.
        assertEquals(3, (int) jdbc.queryForObject(
                "select count(*) from etapas_os where os_id = ?", Integer.class, osId));
        assertEquals(anaId, jdbc.queryForObject(
                "select recurso_id from etapas_os where id = ?", UUID.class, primeira));
    }
}
