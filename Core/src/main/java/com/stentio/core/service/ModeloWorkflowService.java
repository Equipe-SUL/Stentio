package com.stentio.core.service;

import com.stentio.core.dto.ModeloWorkflowRequest;
import com.stentio.core.dto.ModeloWorkflowResponse;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.ModeloWorkflowNaoEncontradoException;
import com.stentio.core.mapper.EtapaOrdemServicoMapper;
import com.stentio.core.model.ModeloWorkflow;
import com.stentio.core.model.ModeloWorkflowEtapa;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.ModeloWorkflowRepository;
import com.stentio.core.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * US-011: cadastro dos modelos de workflow que o gestor aplica às ordens de serviço.
 *
 * <p>O card não descreve este cadastro — o Cenário 2 ficou em branco —, mas sem ele o
 * endpoint de aplicar modelo não teria o que aplicar. É um CRUD mínimo de propósito:
 * criar, listar, consultar, substituir e ativar ou desativar.
 */
@Service
public class ModeloWorkflowService {

    private final ModeloWorkflowRepository modelos;
    private final TipoServicoRepository tiposServico;
    private final EtapaOrdemServicoMapper mapper;

    public ModeloWorkflowService(ModeloWorkflowRepository modelos,
                                 TipoServicoRepository tiposServico,
                                 EtapaOrdemServicoMapper mapper) {
        this.modelos = modelos;
        this.tiposServico = tiposServico;
        this.mapper = mapper;
    }

    @Transactional
    public ModeloWorkflowResponse criar(ModeloWorkflowRequest requisicao) {
        if (modelos.existsByNomeIgnoreCase(requisicao.nome().strip())) {
            throw new EtapasInvalidasException("Já existe um modelo com esse nome");
        }
        ModeloWorkflow modelo = new ModeloWorkflow(requisicao.nome(), requisicao.descricao());
        modelo.definirEtapas(montarEtapas(modelo, requisicao.etapas()));
        return mapper.paraModelo(modelos.save(modelo));
    }

    @Transactional
    public ModeloWorkflowResponse substituir(UUID id, ModeloWorkflowRequest requisicao) {
        ModeloWorkflow modelo = buscar(id);
        if (modelos.existsByNomeIgnoreCaseAndIdNot(requisicao.nome().strip(), id)) {
            throw new EtapasInvalidasException("Já existe um modelo com esse nome");
        }
        // Valida antes de tocar na coleção: um pedido inválido não chega a apagar nada.
        List<ModeloWorkflowEtapa> novas = montarEtapas(modelo, requisicao.etapas());
        modelo.renomear(requisicao.nome(), requisicao.descricao());
        // Sem o flush o Hibernate insere as novas etapas antes de apagar as órfãs e a
        // unique (modelo_id, ordem) estoura mesmo quando a sequência não muda.
        modelo.limparEtapas();
        modelos.flush();
        modelo.definirEtapas(novas);
        return mapper.paraModelo(modelos.save(modelo));
    }

    @Transactional
    public ModeloWorkflowResponse alterarAtivacao(UUID id, boolean ativo) {
        ModeloWorkflow modelo = buscar(id);
        modelo.ativar(ativo);
        return mapper.paraModelo(modelos.save(modelo));
    }

    @Transactional(readOnly = true)
    public List<ModeloWorkflowResponse> listar(Boolean ativo) {
        List<ModeloWorkflow> encontrados = ativo == null
                ? modelos.findAllByOrderByNomeAsc()
                : modelos.findAllByAtivoOrderByNomeAsc(ativo);
        return encontrados.stream().map(mapper::paraModelo).toList();
    }

    @Transactional(readOnly = true)
    public ModeloWorkflowResponse buscarPorId(UUID id) {
        return mapper.paraModelo(buscar(id));
    }

    private ModeloWorkflow buscar(UUID id) {
        return modelos.findWithEtapasById(id)
                .orElseThrow(() -> new ModeloWorkflowNaoEncontradoException(id));
    }

    private List<ModeloWorkflowEtapa> montarEtapas(ModeloWorkflow modelo,
                                                   List<ModeloWorkflowRequest.Etapa> pedidas) {
        SequenciaEtapas.validarOrdens(pedidas.stream().map(ModeloWorkflowRequest.Etapa::ordem).toList());
        Map<UUID, TipoServico> catalogo = carregarTiposServico(
                pedidas.stream().map(ModeloWorkflowRequest.Etapa::tipoServicoId).collect(Collectors.toSet()));
        return pedidas.stream()
                .sorted(Comparator.comparing(ModeloWorkflowRequest.Etapa::ordem))
                .map(e -> new ModeloWorkflowEtapa(modelo, e.ordem(), e.nome(),
                        catalogo.get(e.tipoServicoId()), e.prazoEstimadoHoras()))
                .toList();
    }

    private Map<UUID, TipoServico> carregarTiposServico(Set<UUID> ids) {
        Map<UUID, TipoServico> catalogo = new LinkedHashMap<>();
        tiposServico.findAllById(ids).forEach(t -> catalogo.put(t.getId(), t));
        List<UUID> ausentes = ids.stream().filter(id -> !catalogo.containsKey(id)).toList();
        if (!ausentes.isEmpty()) {
            throw new EtapasInvalidasException("Tipo de serviço não encontrado: " + ausentes.getFirst());
        }
        return catalogo;
    }
}
