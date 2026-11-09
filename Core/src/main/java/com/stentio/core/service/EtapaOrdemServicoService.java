package com.stentio.core.service;

import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.exception.EtapasInvalidasException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.mapper.EtapaOrdemServicoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.SequenciaEtapas;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
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
 * US-011: define a sequência de etapas de uma ordem de serviço.
 *
 * <p>A substituição fica em um método próprio porque a aplicação de modelo (CA.2) vai
 * terminar no mesmo lugar — assim as regras valem igual nos dois caminhos, em vez de
 * cada entrada reimplementar a validação do seu jeito.
 */
@Service
public class EtapaOrdemServicoService {

    private final OrdemServicoRepository ordens;
    private final EtapaOrdemServicoRepository etapas;
    private final TipoServicoRepository tiposServico;
    private final EtapaOrdemServicoMapper mapper;

    public EtapaOrdemServicoService(OrdemServicoRepository ordens,
                                    EtapaOrdemServicoRepository etapas,
                                    TipoServicoRepository tiposServico,
                                    EtapaOrdemServicoMapper mapper) {
        this.ordens = ordens;
        this.etapas = etapas;
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
