package com.stentio.core.config;

import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.test-data.enabled", havingValue = "true")
public class TestDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);

    private final ClienteRepository clienteRepository;
    private final CategoriaProjetoRepository categoriaProjetoRepository;
    private final IdiomaRepository idiomaRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final OrcamentoRepository orcamentoRepository;

    public TestDataSeeder(ClienteRepository clienteRepository,
                          CategoriaProjetoRepository categoriaProjetoRepository,
                          IdiomaRepository idiomaRepository,
                          TipoServicoRepository tipoServicoRepository,
                          SolicitacaoRepository solicitacaoRepository,
                          OrcamentoRepository orcamentoRepository) {
        this.clienteRepository = clienteRepository;
        this.categoriaProjetoRepository = categoriaProjetoRepository;
        this.idiomaRepository = idiomaRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[TestDataSeeder] Criando dados de teste...");

        // Garantir catálogos básicos
        CategoriaProjeto catTrad = ensureCategoria("Tradução Técnica");
        CategoriaProjeto catJur = ensureCategoria("Tradução Jurídica");
        CategoriaProjeto catCom = ensureCategoria("Tradução Comercial");

        Idioma pt = ensureIdioma("Português", "pt-BR");
        Idioma en = ensureIdioma("Inglês", "en");
        Idioma es = ensureIdioma("Espanhol", "es");
        Idioma fr = ensureIdioma("Francês", "fr");

        ensureTipoServico("Tradução", "Tradução de documentos e conteúdos gerais.");
        ensureTipoServico("Revisão", "Revisão ortográfica, gramatical e de estilo.");
        ensureTipoServico("Formatação", "Formatação e padronização de documentos.");

        // Clientes
        Cliente c1 = saveCliente("TechNova Soluções Ltda", "Marina Silva", "marina@technova.com.br", "(11) 99999-1001", "11222333000181");
        Cliente c2 = saveCliente("GlobalComex Importação e Exportação", "Carlos Mendes", "carlos@globalcomex.com.br", "(21) 98888-2002", "22333444000192");
        Cliente c3 = saveCliente("Advocacia Martins & Associados", "Juliana Martins", "juliana@martinsadv.com.br", "(31) 97777-3003", "33444555000103");
        Cliente c4 = saveCliente("MediCare Equipamentos Médicos", "Rafael Torres", "rafael@medicare.com.br", "(41) 96666-4004", "44555666000114");
        Cliente c5 = saveCliente("EuroTrade Brasil", "Fernanda Costa", "fernanda@eurotrade.com.br", "(51) 95555-5005", "55666777000125");

        // Solicitações
        Solicitacao s1 = saveSolicitacao(c1, catTrad, en, pt, "Manual técnico de equipamentos industriais - 25 páginas");
        Solicitacao s2 = saveSolicitacao(c2, catCom, en, pt, "Catálogo de produtos para lançamento no Brasil");
        Solicitacao s3 = saveSolicitacao(c3, catJur, es, pt, "Contrato de parceria comercial - revisão e tradução juramentada");
        Solicitacao s4 = saveSolicitacao(c4, catTrad, en, es, "Instruções de uso de equipamento médico");
        Solicitacao s5 = saveSolicitacao(c5, catCom, fr, pt, "Material publicitário e apresentações comerciais");

        // Orçamentos
        Orcamento o1 = saveOrcamento(c1, s1, en, pt, BigDecimal.valueOf(1250.00), StatusOrcamento.APROVADO,
                "Tradução", 25, UnidadeCobranca.PAGINA, BigDecimal.valueOf(40.00),
                new SeedServico("Revisão", 25, UnidadeCobranca.PAGINA, BigDecimal.valueOf(10.00)));
        Orcamento o2 = saveOrcamento(c2, s2, en, pt, BigDecimal.valueOf(890.50), StatusOrcamento.AGUARDANDO_APROVACAO,
                "Tradução", 1781, UnidadeCobranca.PALAVRA, BigDecimal.valueOf(0.50));
        Orcamento o3 = saveOrcamento(c3, s3, es, pt, BigDecimal.valueOf(2150.00), StatusOrcamento.REJEITADO,
                "Tradução", 20, UnidadeCobranca.PAGINA, BigDecimal.valueOf(80.00),
                new SeedServico("Revisão", 20, UnidadeCobranca.PAGINA, BigDecimal.valueOf(27.50)));
        Orcamento o4 = saveOrcamento(c4, s4, en, es, BigDecimal.valueOf(980.00), StatusOrcamento.EM_ELABORACAO,
                "Tradução", 14, UnidadeCobranca.PAGINA, BigDecimal.valueOf(60.00),
                new SeedServico("Formatação", 14, UnidadeCobranca.PAGINA, BigDecimal.valueOf(10.00)));
        Orcamento o5 = saveOrcamento(c5, s5, fr, pt, BigDecimal.valueOf(1491.25), StatusOrcamento.APROVADO,
                "Tradução", 1200, UnidadeCobranca.PALAVRA, BigDecimal.valueOf(0.90),
                new SeedServico("Formatação", 7, UnidadeCobranca.PAGINA, BigDecimal.valueOf(58.75)));

        orcamentoRepository.saveAll(List.of(o1, o2, o3, o4, o5));

        log.info("[TestDataSeeder] Seed concluído! Clientes: {}, Solicitações: {}, Orçamentos: {}",
                clienteRepository.count(), solicitacaoRepository.count(), orcamentoRepository.count());
    }

    private CategoriaProjeto ensureCategoria(String nome) {
        return categoriaProjetoRepository.findAll().stream()
                .filter(c -> nome.equalsIgnoreCase(c.getNome()))
                .findFirst()
                .orElseGet(() -> {
                    CategoriaProjeto c = new CategoriaProjeto();
                    c.setNome(nome);
                    return categoriaProjetoRepository.save(c);
                });
    }

    private Idioma ensureIdioma(String nome, String codigoIso) {
        return idiomaRepository.findAll().stream()
                .filter(i -> codigoIso.equalsIgnoreCase(i.getCodigoIso()))
                .findFirst()
                .orElseGet(() -> {
                    Idioma i = new Idioma();
                    i.setNome(nome);
                    i.setCodigoIso(codigoIso);
                    i.setAtivo(true);
                    return idiomaRepository.save(i);
                });
    }

    private TipoServico ensureTipoServico(String nome, String descricao) {
        return tipoServicoRepository.findAll().stream()
                .filter(servico -> nome.equalsIgnoreCase(servico.getNome()))
                .findFirst()
                .orElseGet(() -> {
                    TipoServico servico = new TipoServico();
                    servico.setNome(nome);
                    servico.setDescricao(descricao);
                    servico.setAtivo(true);
                    return tipoServicoRepository.save(servico);
                });
    }

    private Cliente saveCliente(String nomeEmpresa, String nomeRepresentante, String email, String telefone, String cnpj) {
        if (clienteRepository.findByCpfCnpj(cnpj).isPresent()) {
            return clienteRepository.findByCpfCnpj(cnpj).get();
        }
        Cliente cliente = new Cliente(nomeEmpresa, nomeRepresentante, email, telefone, cnpj);
        return clienteRepository.save(cliente);
    }

    private Solicitacao saveSolicitacao(Cliente cliente, CategoriaProjeto cat, Idioma origem, Idioma destino, String descricao) {
        return solicitacaoRepository.findAll().stream()
                .filter(existing -> existing.getCliente().getId().equals(cliente.getId()))
                .filter(existing -> descricao.equals(existing.getDescricao()))
                .findFirst()
                .orElseGet(() -> solicitacaoRepository.save(
                        new Solicitacao(cliente, cat, origem, destino, descricao)));
    }

    private Orcamento saveOrcamento(Cliente cliente, Solicitacao solicitacao,
                                    Idioma idiomaOrigem, Idioma idiomaDestino,
                                    BigDecimal valor, StatusOrcamento status,
                                    String tipoServico1, int quantidade1, UnidadeCobranca unidade1,
                                    BigDecimal valorUnitario1, SeedServico... servicosExtras) {
        Orcamento orcamento = orcamentoRepository.findAll().stream()
                .filter(existing -> existing.getSolicitacao() != null)
                .filter(existing -> existing.getSolicitacao().getId().equals(solicitacao.getId()))
                .findFirst()
                .map(existing -> {
                    existing.setStatus(status);
                    existing.setValorTotal(valor);
                    return existing;
                })
                .orElseGet(() -> orcamentoRepository.save(createOrcamento(cliente, solicitacao, valor, status)));

        if (orcamento.getItens().isEmpty()) {
            OrcamentoItem item = new OrcamentoItem();
            item.setOrdem(1);
            item.setIdiomaOrigem(idiomaOrigem);
            item.setIdiomaDestino(idiomaDestino);
            item.setSubtotalItem(valor);
            item.adicionarServico(createServico(tipoServico1, quantidade1, unidade1, valorUnitario1));

            if (servicosExtras.length > 0) {
                SeedServico servicoExtra = servicosExtras[0];
                item.adicionarServico(createServico(
                        servicoExtra.tipo(),
                        servicoExtra.quantidade(),
                        servicoExtra.unidade(),
                        servicoExtra.valorUnitario()));
            }

            orcamento.adicionarItem(item);
            orcamentoRepository.save(orcamento);
        }
        return orcamento;
    }

    private OrcamentoItemServico createServico(String tipoServico, int quantidade,
                                               UnidadeCobranca unidade, BigDecimal valorUnitario) {
        OrcamentoItemServico servico = new OrcamentoItemServico();
        servico.setTipoServico(tipoServico);
        servico.setQuantidade(BigDecimal.valueOf(quantidade));
        servico.setUnidade(unidade);
        servico.setValorUnitario(valorUnitario);
        servico.setSubtotal(valorUnitario.multiply(BigDecimal.valueOf(quantidade)));
        return servico;
    }

    private record SeedServico(String tipo, int quantidade, UnidadeCobranca unidade,
                               BigDecimal valorUnitario) {
    }

    private Orcamento createOrcamento(Cliente cliente, Solicitacao solicitacao, BigDecimal valor,
                                      StatusOrcamento status) {
        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setSolicitacao(solicitacao);
        orcamento.setStatus(status);
        orcamento.setValorTotal(valor);
        orcamento.setNumeroVersao(1);
        orcamento.setVersao(0);
        return orcamento;
    }
}
