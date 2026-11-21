package com.stentio.core.service;

import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.RecursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Quem pode ser alocado a uma etapa: ativo, verificado, com o par de idiomas da
 * solicitação e o tipo de serviço da etapa.
 *
 * <p>O par e o tipo saem da própria etapa, e não do front. Antes a tela de alocação listava
 * o catálogo inteiro de recursos e o gestor só descobria a incompatibilidade ao confirmar
 * ("não tem o par English → Espanhol nas competências"). É a mesma consulta usada pelo
 * disparo e o mesmo critério que a alocação valida no servidor, então a lista oferecida e a
 * regra aplicada não têm como divergir.
 */
@Service
@Transactional(readOnly = true)
public class RecursosElegiveisEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final RecursoRepository recursos;
    private final RecursoMapper mapper;

    public RecursosElegiveisEtapaService(EtapaOrdemServicoRepository etapas,
                                         RecursoRepository recursos,
                                         RecursoMapper mapper) {
        this.etapas = etapas;
        this.recursos = recursos;
        this.mapper = mapper;
    }

    public List<RecursoDisponivelResponse> listar(UUID ordemServicoId, UUID etapaId) {
        EtapaOrdemServico etapa = etapas.buscarParaAlocacao(ordemServicoId, etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        UUID origemId = solicitacao.getIdiomaOrigem().getId();
        UUID destinoId = solicitacao.getIdiomaDestino().getId();

        return recursos.buscarDisponiveis(origemId, destinoId, etapa.getTipoServico().getId()).stream()
                .map(recurso -> mapper.paraDisponivel(recurso, origemId, destinoId))
                .toList();
    }
}
