package com.stentio.core.service;

import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.ModeloWorkflowNaoEncontradoException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.mapper.EtapaOrdemServicoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.ModeloWorkflow;
import com.stentio.core.model.ModeloWorkflowEtapa;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.ModeloWorkflowRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import com.stentio.core.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * US-011: define a sequência de etapas de uma ordem de serviço, montada à mão (CA.3) ou
 * clonada de um modelo (CA.2).
 *
 * <p>Os dois caminhos terminam no mesmo lugar: substituem a sequência inteira. Guardar
 * essa substituição em um método só garante que as regras valham igual nos dois, em vez
 * de cada entrada reimplementar a validação do seu jeito.
 */
@Service
public class EtapaOrdemServicoService {

    private final OrdemServicoRepository ordens;
    private final EtapaOrdemServicoRepository etapas;
    private final ModeloWorkflowRepository modelos;
    private final TipoServicoRepository tiposServico;
    private final EtapaOrdemServicoMapper mapper;

    public EtapaOrdemServicoService(OrdemServicoRepository ordens,
                                    EtapaOrdemServicoRepository etapas,
                                    ModeloWorkflowRepository modelos,
                                    TipoServicoRepository tiposServico,
                                    EtapaOrdemServicoMapper mapper) {
        this.ordens = ordens;
        this.etapas = etapas;
        this.modelos = modelos;
        this.tiposServico = tiposServico;
        this.mapper = mapper;
    }

    // CA.3 e Cenário 1
    @Transactional
    public EtapasOrdemServicoResponse definirManualmente(UUID ordemServicoId, EtapasOrdemServicoRequest requisicao) {
        OrdemServico ordem = buscarOrdem(ordemServicoId);

        List<EtapasOrdemServicoRequest.Etapa> pedidas = requisicao.etapas();
        SequenciaEtapas.validarOrdens(pedidas.stream().map(EtapasOrdemServicoRequest.Etapa::ordem).toList());

        Map<UUID, TipoServico> catalogo = carregarTiposServico(
                pedidas.stream().map(EtapasOrdemServicoRequest.Etapa::tipoServicoId).collect(java.util.stream.Collectors.toSet()));

        List<EtapaOrdemServico> novas = pedidas.stream()
                .sorted(java.util.Comparator.comparing(EtapasOrdemServicoRequest.Etapa::ordem))
                .map(e -> new EtapaOrdemServico(ordem, e.ordem(), e.nome(),
                        catalogo.get(e.tipoServicoId()), e.prazoEstimadoHoras()))
                .toList();

        return substituirSequencia(ordem, novas);
    }

    // CA.2
    @Transactional
    public EtapasOrdemServicoResponse aplicarModelo(UUID ordemServicoId, UUID modeloId) {
        OrdemServico ordem = buscarOrdem(ordemServicoId);

        ModeloWorkflow modelo = modelos.findWithEtapasById(modeloId)
                .orElseThrow(() -> new ModeloWorkflowNaoEncontradoException(modeloId));
        if (Boolean.FALSE.equals(modelo.getAtivo())) {
            throw new ModeloWorkflowNaoEncontradoException(
                    "Modelo de workflow inativo e por isso indisponível: " + modelo.getNome());
        }

        List<ModeloWorkflowEtapa> molde = modelo.getEtapas();
        if (molde.isEmpty()) {
            throw new EtapasInvalidasException("O modelo " + modelo.getNome() + " não tem etapas cadastradas");
        }
        // O modelo foi validado ao ser salvo, mas um tipo de serviço pode ter sido desativado
        // desde então: revalidar aqui evita clonar uma sequência que já nasce inconsistente.
        garantirTiposAtivos(molde.stream().map(ModeloWorkflowEtapa::getTipoServico).toList());

        List<EtapaOrdemServico> novas = molde.stream()
                .map(m -> new EtapaOrdemServico(ordem, m.getOrdem(), m.getNome(),
                        m.getTipoServico(), m.getPrazoEstimadoHoras()))
                .toList();

        return substituirSequencia(ordem, novas);
    }

    private EtapasOrdemServicoResponse substituirSequencia(OrdemServico ordem, List<EtapaOrdemServico> novas) {
        etapas.apagarPorOrdemServico(ordem.getId());
        List<EtapaOrdemServico> gravadas = etapas.saveAll(novas);
        ordem.planejar();
        ordens.save(ordem);
        return mapper.paraResposta(ordem, gravadas);
    }

    private OrdemServico buscarOrdem(UUID id) {
        return ordens.findById(id).orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }

    private Map<UUID, TipoServico> carregarTiposServico(Set<UUID> ids) {
        Map<UUID, TipoServico> catalogo = new LinkedHashMap<>();
        tiposServico.findAllById(ids).forEach(t -> catalogo.put(t.getId(), t));
        List<UUID> ausentes = ids.stream().filter(id -> !catalogo.containsKey(id)).toList();
        if (!ausentes.isEmpty()) {
            throw new EtapasInvalidasException("Tipo de serviço não encontrado: " + ausentes.getFirst());
        }
        garantirTiposAtivos(catalogo.values().stream().toList());
        return catalogo;
    }

    private void garantirTiposAtivos(List<TipoServico> tipos) {
        tipos.stream()
                .filter(t -> Boolean.FALSE.equals(t.getAtivo()))
                .findFirst()
                .ifPresent(t -> {
                    throw new EtapasInvalidasException(
                            "O tipo de serviço " + t.getNome() + " está inativo e não pode ser usado em uma etapa");
                });
    }
}
