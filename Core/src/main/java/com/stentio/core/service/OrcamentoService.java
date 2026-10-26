package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoCriacaoRequest;
import com.stentio.core.dto.OrcamentoDetalheResponse;
import com.stentio.core.dto.OrcamentoItensRequest;
import com.stentio.core.dto.OrcamentoPaginaResponse;
import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEditavelException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.exception.ReferenciaInvalidaException;
import com.stentio.core.exception.SolicitacaoNaoEncontradaException;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrcamentoItem;
import com.stentio.core.model.OrcamentoItemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrcamentoSpecifications;
import com.stentio.core.repository.SolicitacaoRepository;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// US-11: cadastro de orçamento vinculado a uma solicitação.
@Service
@Transactional(readOnly = true)
public class OrcamentoService {

    static final String MENSAGEM_IDIOMAS_IGUAIS = "O idioma de origem não pode ser igual ao idioma de destino";
    static final String MENSAGEM_EXPIRACAO = "A data de expiração deve ser posterior à data de criação";
    static final String MENSAGEM_SOLICITACAO_DE_OUTRO_CLIENTE = "A solicitação informada não pertence ao cliente";
    static final String MENSAGEM_PAGINA = "O parâmetro 'pagina' deve ser maior ou igual a 1";
    static final String MENSAGEM_TAMANHO_PAGINA = "O parâmetro 'tamanho_pagina' deve estar entre 1 e " + OrcamentoService.TAMANHO_MAXIMO_PAGINA;
    static final int TAMANHO_MAXIMO_PAGINA = 100;

    // Mais recentes primeiro; o id desempata orçamentos criados no mesmo instante.
    private static final Sort ORDENACAO_PADRAO = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.asc("id"));

    private final OrcamentoRepository orcamentos;
    private final SolicitacaoRepository solicitacoes;
    private final ClienteRepository clientes;
    private final IdiomaRepository idiomas;
    private final CalculoOrcamentoService calculo;
    private final Clock clock;
    private final Validator validator;

    public OrcamentoService(OrcamentoRepository orcamentos,
                            SolicitacaoRepository solicitacoes,
                            ClienteRepository clientes,
                            IdiomaRepository idiomas,
                            CalculoOrcamentoService calculo,
                            Clock clock,
                            Validator validator) {
        this.orcamentos = orcamentos;
        this.solicitacoes = solicitacoes;
        this.clientes = clientes;
        this.idiomas = idiomas;
        this.calculo = calculo;
        this.clock = clock;
        this.validator = validator;
    }

    // CA.1: nasce EM_ELABORACAO, versão 1 e total zero.
    @Transactional
    public OrcamentoDetalheResponse criar(OrcamentoCriacaoRequest request) {
        // RN.5
        if (!request.dataExpiracao().isAfter(clock.instant())) {
            throw new OrcamentoInvalidoException(MENSAGEM_EXPIRACAO);
        }

        // RN.1: a solicitação (US-09) e o cliente precisam existir.
        Solicitacao solicitacao = solicitacoes.buscarPorIdComLock(request.solicitacaoId())
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(request.solicitacaoId()));
        Cliente cliente = clientes.findById(request.clienteId())
                .orElseThrow(() -> new ReferenciaInvalidaException("Cliente", request.clienteId()));
        if (!solicitacao.getCliente().getId().equals(cliente.getId())) {
            throw new OrcamentoInvalidoException(MENSAGEM_SOLICITACAO_DE_OUTRO_CLIENTE);
        }
        // Também bloqueia solicitações antigas cujo status não foi atualizado ao criar o orçamento.
        if (orcamentos.existsBySolicitacaoId(solicitacao.getId())) {
            throw new OrcamentoInvalidoException("A solicitação já possui um orçamento. Continue pelo orçamento existente.");
        }
        solicitacao.iniciarOrcamento();

        Orcamento orcamento = new Orcamento();
        orcamento.setSolicitacao(solicitacao);
        orcamento.setCliente(cliente);
        orcamento.setStatus(StatusOrcamento.EM_ELABORACAO);
        orcamento.setNumeroVersao(1);
        orcamento.setValorTotal(BigDecimal.ZERO.setScale(2));
        orcamento.setDataExpiracao(request.dataExpiracao());

        return OrcamentoDetalheResponse.de(orcamentos.save(orcamento));
    }

    // Substitui todos os itens do orçamento e recalcula os totais na mesma transação.
    @Transactional
    public OrcamentoDetalheResponse substituirItens(UUID orcamentoId, OrcamentoItensRequest request) {
        // Valida tudo antes de gravar: em caso de erro nada é persistido.
        validarItens(request);

        Orcamento orcamento = orcamentos.buscarPorIdComLock(orcamentoId)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(orcamentoId));
        if (orcamento.getStatus() != StatusOrcamento.EM_ELABORACAO) {
            throw new OrcamentoNaoEditavelException();
        }
        Map<UUID, Idioma> idiomasPorId = carregarIdiomas(request);

        // O Hibernate executa INSERTs antes dos DELETEs de órfãos no flush. Sem este flush, reenviar
        // a mesma "ordem" violaria uk_orcamento_item_ordem antes de os itens antigos serem apagados.
        orcamento.getItens().clear();
        orcamentos.flush();

        request.itens().stream()
                .sorted(Comparator.comparing(OrcamentoItensRequest.Item::ordem))
                .forEach(dados -> orcamento.adicionarItem(novoItem(dados, idiomasPorId)));

        // CA.3 e CA.4: valores enviados pelo cliente nunca são usados como subtotal.
        calculo.recalcular(orcamento);
        return OrcamentoDetalheResponse.de(orcamentos.saveAndFlush(orcamento));
    }

    // CA.5: filtros opcionais e combináveis. "pagina" chega em base 1.
    public OrcamentoPaginaResponse listar(StatusOrcamento status, UUID clienteId, UUID solicitacaoId,
                                          int pagina, int tamanhoPagina) {
        if (pagina < 1) {
            throw new OrcamentoInvalidoException(MENSAGEM_PAGINA);
        }
        if (tamanhoPagina < 1 || tamanhoPagina > TAMANHO_MAXIMO_PAGINA) {
            throw new OrcamentoInvalidoException(MENSAGEM_TAMANHO_PAGINA);
        }

        PageRequest paginacao = PageRequest.of(pagina - 1, tamanhoPagina, ORDENACAO_PADRAO);
        return OrcamentoPaginaResponse.de(
                orcamentos.findAll(OrcamentoSpecifications.comFiltros(status, clienteId, solicitacaoId), paginacao));
    }

    // CA.6: hierarquia completa, itens ordenados por "ordem".
    public OrcamentoDetalheResponse buscarPorId(UUID id) {
        return orcamentos.buscarDetalhePorId(id)
                .map(OrcamentoDetalheResponse::de)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
    }

    private void validarItens(OrcamentoItensRequest request) {
        // Reutiliza as restrições do DTO também para chamadas diretas ao serviço.
        var erros = validator.validate(request);
        if (!erros.isEmpty()) {
            throw new OrcamentoInvalidoException(erros.stream()
                    .map(erro -> erro.getMessage()).distinct().sorted().collect(Collectors.joining("; ")));
        }
        Set<Integer> ordens = new HashSet<>();
        for (OrcamentoItensRequest.Item item : request.itens()) {
            // RN.2 / CA.2
            if (item.idiomaOrigemId().equals(item.idiomaDestinoId())) {
                throw new OrcamentoInvalidoException(MENSAGEM_IDIOMAS_IGUAIS);
            }
            if (!ordens.add(item.ordem())) {
                throw new OrcamentoInvalidoException("A ordem " + item.ordem() + " está repetida entre os itens");
            }
        }
    }

    private Map<UUID, Idioma> carregarIdiomas(OrcamentoItensRequest request) {
        Set<UUID> ids = new LinkedHashSet<>();
        request.itens().forEach(item -> {
            ids.add(item.idiomaOrigemId());
            ids.add(item.idiomaDestinoId());
        });

        Map<UUID, Idioma> encontrados = idiomas.findAllById(ids).stream()
                .collect(Collectors.toMap(Idioma::getId, Function.identity()));
        ids.stream()
                .filter(id -> !encontrados.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new ReferenciaInvalidaException("Idioma", id);
                });
        return encontrados;
    }

    private static OrcamentoItem novoItem(OrcamentoItensRequest.Item dados, Map<UUID, Idioma> idiomasPorId) {
        OrcamentoItem item = new OrcamentoItem();
        item.setOrdem(dados.ordem());
        item.setIdiomaOrigem(idiomasPorId.get(dados.idiomaOrigemId()));
        item.setIdiomaDestino(idiomasPorId.get(dados.idiomaDestinoId()));
        dados.servicos().forEach(servicoDados -> {
            OrcamentoItemServico servico = new OrcamentoItemServico();
            servico.setTipoServico(servicoDados.tipoServico().trim());
            servico.setQuantidade(servicoDados.quantidade());
            servico.setUnidade(servicoDados.unidade());
            servico.setValorUnitario(servicoDados.valorUnitario());
            item.adicionarServico(servico);
        });
        return item;
    }
}
