package com.stentio.core.controller;

import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import com.stentio.core.service.OrdemServicoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-17 ponta a ponta: HTTP (controller + segurança) até o Postgres real, passando por
 * Specification, join cliente, paginação e mapper. Usa o mesmo opt-in e banco de testes
 * da US-14/US-16. As ordens nascem pelo fluxo de produção da US-16, não por insert cru.
 */
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
@AutoConfigureMockMvc
@WithMockUser(username = "gestor@stentio.com", roles = {"GESTOR_PROJETO"})
class OrdemServicoConsultaPostgresTest {

    private static final String BASE_PATH = "/api/v1/ordens-servico";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdemServicoService ordemServicoService;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private CategoriaProjetoRepository categorias;

    @Autowired
    private IdiomaRepository idiomas;

    @Autowired
    private SolicitacaoRepository solicitacoes;

    @Autowired
    private OrcamentoRepository orcamentos;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID clienteAId;
    private UUID clienteBId;
    private UUID categoriaId;
    private final List<UUID> idiomasIds = new ArrayList<>();
    private final List<UUID> solicitacoesIds = new ArrayList<>();
    private final List<UUID> orcamentosIds = new ArrayList<>();
    private final List<UUID> ordensIds = new ArrayList<>();
    private final List<UUID> arquivosIds = new ArrayList<>();

    private UUID osA1Id;
    private UUID osA2Id;
    private UUID osB1Id;
    private UUID orcamentoA1Id;
    private UUID solicitacaoComArquivoId;
    private UUID arquivoId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cliente clienteA = clientes.saveAndFlush(new Cliente("Empresa A R3", "Representante A",
                    UUID.randomUUID() + "@teste.invalid", "11999999999", cpfValido()));
            clienteAId = clienteA.getId();
            Cliente clienteB = clientes.saveAndFlush(new Cliente("Empresa B R3", "Representante B",
                    UUID.randomUUID() + "@teste.invalid", "11888888888", cpfValido()));
            clienteBId = clienteB.getId();

            CategoriaProjeto categoria = new CategoriaProjeto();
            categoria.setNome("Categoria R3 " + UUID.randomUUID());
            categoriaId = categorias.saveAndFlush(categoria).getId();

            Idioma origem = novoIdioma("Português");
            Idioma destino = novoIdioma("Inglês");
            idiomasIds.add(origem.getId());
            idiomasIds.add(destino.getId());

            orcamentoA1Id = criarOrcamentoAprovado(clienteA, categoria, origem, destino, "Contrato R3", true);
            criarOrcamentoAprovado(clienteA, categoria, origem, destino, "Segundo projeto R3", false);
            criarOrcamentoAprovado(clienteB, categoria, origem, destino, "Projeto da empresa B", false);
        });

        // Criadas pelo fluxo real da US-16, cada uma em sua própria transação.
        osA1Id = ordemServicoService.criarParaOrcamentoAprovado(orcamentoA1Id).getId();
        osA2Id = ordemServicoService.criarParaOrcamentoAprovado(orcamentosIds.get(1)).getId();
        osB1Id = ordemServicoService.criarParaOrcamentoAprovado(orcamentosIds.get(2)).getId();
        ordensIds.addAll(List.of(osA1Id, osA2Id, osB1Id));

        // Datas controladas para a ordenação ser determinística (inserts no mesmo instante empatariam).
        ajustarCriadoEm(osA1Id, OffsetDateTime.of(2026, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        ajustarCriadoEm(osA2Id, OffsetDateTime.of(2026, 2, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        ajustarCriadoEm(osB1Id, OffsetDateTime.of(2026, 3, 10, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("CA.2: lista as três OS em ordem decrescente com o envelope paginado")
    void deveListarTodasEmOrdemDecrescente() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho_pagina").value(10))
                .andExpect(jsonPath("$.total_elementos").value(3))
                .andExpect(jsonPath("$.conteudo.length()").value(3))
                .andExpect(jsonPath("$.conteudo[0].id").value(osB1Id.toString()))
                .andExpect(jsonPath("$.conteudo[1].id").value(osA2Id.toString()))
                .andExpect(jsonPath("$.conteudo[2].id").value(osA1Id.toString()))
                .andExpect(jsonPath("$.conteudo[0].codigo_visual").value(codigoEsperado(osB1Id)))
                .andExpect(jsonPath("$.conteudo[0].status").value("PENDENTE_ALOCACAO"))
                .andExpect(jsonPath("$.conteudo[0].cliente_nome").value("Empresa B R3"))
                .andExpect(jsonPath("$.conteudo[0].data_criacao").value(startsWith("2026-03-10")));
    }

    @Test
    @DisplayName("CA.2: pagina em base 1 devolvendo o item mais antigo na segunda página")
    void devePaginar() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("pagina", "2").param("tamanho_pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.tamanho_pagina").value(2))
                .andExpect(jsonPath("$.total_elementos").value(3))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(osA1Id.toString()));
    }

    @Test
    @DisplayName("CA.1: filtra por cliente_id e pelo alias cliente com o mesmo resultado")
    void deveFiltrarPorClienteEAlia() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("cliente_id", clienteAId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(osA2Id.toString()))
                .andExpect(jsonPath("$.conteudo[1].id").value(osA1Id.toString()));

        // Regressão da R2: o alias "cliente" precisa filtrar igual a "cliente_id".
        mockMvc.perform(get(BASE_PATH).param("cliente", clienteAId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(osA2Id.toString()))
                .andExpect(jsonPath("$.conteudo[1].id").value(osA1Id.toString()));
    }

    @Test
    @DisplayName("CA.1: combina status com cliente")
    void deveCombinarStatusECliente() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("status", "PENDENTE_ALOCACAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(3));

        mockMvc.perform(get(BASE_PATH)
                        .param("status", "PENDENTE_ALOCACAO")
                        .param("cliente_id", clienteAId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elementos").value(2));
    }

    @Test
    @DisplayName("CA.3: detalhe carrega os vínculos e o anexo mesmo com open-in-view desligado")
    void deveRetornarDetalheComVinculosEArquivo() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/" + osA1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(osA1Id.toString()))
                .andExpect(jsonPath("$.codigo").value(codigoEsperado(osA1Id)))
                .andExpect(jsonPath("$.status").value("PENDENTE_ALOCACAO"))
                .andExpect(jsonPath("$.orcamento_id").value(orcamentoA1Id.toString()))
                .andExpect(jsonPath("$.cliente.id").value(clienteAId.toString()))
                .andExpect(jsonPath("$.cliente.nome").value("Empresa A R3"))
                .andExpect(jsonPath("$.solicitacao.id").value(solicitacaoComArquivoId.toString()))
                .andExpect(jsonPath("$.solicitacao.descricao").value("Contrato R3"))
                .andExpect(jsonPath("$.solicitacao.idioma_origem").value("Português"))
                .andExpect(jsonPath("$.solicitacao.idioma_destino").value("Inglês"))
                .andExpect(jsonPath("$.solicitacao.arquivos.length()").value(1))
                .andExpect(jsonPath("$.solicitacao.arquivos[0].id").value(arquivoId.toString()))
                .andExpect(jsonPath("$.solicitacao.arquivos[0].nome").value("contrato.pdf"))
                .andExpect(jsonPath("$.solicitacao.arquivos[0].url").value(
                        "/api/v1/solicitacoes/" + solicitacaoComArquivoId + "/arquivos/" + arquivoId));
    }

    @Test
    @DisplayName("CA.3: devolve 404 para OS inexistente")
    void deveRetornarNotFound() throws Exception {
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(get(BASE_PATH + "/" + inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de serviço não encontrada: " + inexistente));
    }

    @Test
    @DisplayName("CA.2: devolve 400 para página e status inválidos")
    void deveRetornarBadRequest() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("pagina", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A página deve ser maior ou igual a 1"));

        mockMvc.perform(get(BASE_PATH).param("status", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "atendente@stentio.com", roles = {"ATENDENTE"})
    @DisplayName("Devolve 403 para perfil sem permissão na rota de OS")
    void deveRetornarForbiddenParaPerfilSemPermissao() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Devolve 401 sem token")
    void deveRetornarUnauthorizedSemToken() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isUnauthorized());
    }

    @AfterEach
    void limparSomenteDadosDesteTeste() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (UUID arquivo : arquivosIds) {
                jdbc.update("delete from arquivos_solicitacao where id = ?", arquivo);
            }
            for (UUID ordem : ordensIds) {
                jdbc.update("delete from ordens_servico where id = ?", ordem);
            }
            for (UUID orcamento : orcamentosIds) {
                jdbc.update("delete from orcamento_email_outbox where orcamento_id = ?", orcamento);
                jdbc.update("delete from orcamento_decisao_outbox where orcamento_id = ?", orcamento);
                jdbc.update("delete from orcamento_historico where orcamento_id = ?", orcamento);
                jdbc.update("delete from orcamentos where id = ?", orcamento);
            }
            for (UUID solicitacao : solicitacoesIds) {
                jdbc.update("delete from solicitacoes where id = ?", solicitacao);
            }
            if (clienteAId != null) {
                jdbc.update("delete from clientes where id = ?", clienteAId);
            }
            if (clienteBId != null) {
                jdbc.update("delete from clientes where id = ?", clienteBId);
            }
            if (categoriaId != null) {
                jdbc.update("delete from categorias_projeto where id = ?", categoriaId);
            }
            for (UUID idioma : idiomasIds) {
                jdbc.update("delete from idiomas where id = ?", idioma);
            }
        });
    }

    private UUID criarOrcamentoAprovado(Cliente cliente, CategoriaProjeto categoria, Idioma origem,
                                        Idioma destino, String descricao, boolean comArquivo) {
        Solicitacao solicitacao = new Solicitacao(cliente, categoria, origem, destino, descricao);
        if (comArquivo) {
            solicitacao.anexarArquivo(new ArquivoArmazenado(
                    "contrato.pdf", "2026/10/" + UUID.randomUUID() + ".pdf", "application/pdf", 1234));
        }
        solicitacoes.saveAndFlush(solicitacao);
        solicitacoesIds.add(solicitacao.getId());

        if (comArquivo) {
            ArquivoSolicitacao arquivo = solicitacao.getArquivos().getFirst();
            solicitacaoComArquivoId = solicitacao.getId();
            arquivoId = arquivo.getId();
            arquivosIds.add(arquivo.getId());
        }

        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setSolicitacao(solicitacao);
        orcamento.setStatus(StatusOrcamento.APROVADO);
        orcamentos.saveAndFlush(orcamento);
        orcamentosIds.add(orcamento.getId());
        return orcamento.getId();
    }

    private Idioma novoIdioma(String nome) {
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(UUID.randomUUID().toString().substring(0, 8));
        return idiomas.saveAndFlush(idioma);
    }

    private void ajustarCriadoEm(UUID ordemId, OffsetDateTime criadoEm) {
        jdbc.update("update ordens_servico set criado_em = ? where id = ?", criadoEm, ordemId);
    }

    // Mesma regra do OrdemServicoMapper: ano de criação em UTC + 6 primeiros caracteres do id.
    private static String codigoEsperado(UUID id) {
        return "OS-2026-" + id.toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    // O caminho de persistência direto só normaliza os dígitos, mas geramos um CPF válido
    // e único por execução para não colidir com seeds nem com outros testes.
    private static String cpfValido() {
        while (true) {
            int[] digitos = new int[11];
            for (int i = 0; i < 9; i++) {
                digitos[i] = ThreadLocalRandom.current().nextInt(10);
            }
            digitos[9] = digitoVerificador(digitos, 9, 10);
            digitos[10] = digitoVerificador(digitos, 10, 11);

            StringBuilder sb = new StringBuilder(11);
            for (int digito : digitos) {
                sb.append(digito);
            }
            String cpf = sb.toString();
            if (!cpf.chars().allMatch(c -> c == cpf.charAt(0))) {
                return cpf;
            }
        }
    }

    private static int digitoVerificador(int[] digitos, int quantidade, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (pesoInicial - i);
        }
        int resto = 11 - (soma % 11);
        return resto >= 10 ? 0 : resto;
    }
}
