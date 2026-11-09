package com.stentio.core.mapper;

import com.stentio.core.dto.EtapaOrdemServicoResponse;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.dto.ModeloWorkflowResponse;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.ModeloWorkflow;
import com.stentio.core.model.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.List;

// Converte entidade em resposta. Não consulta o banco: quem carrega as entidades é o service.
@Component
public class EtapaOrdemServicoMapper {

    public EtapasOrdemServicoResponse paraResposta(OrdemServico ordem, List<EtapaOrdemServico> etapas) {
        return new EtapasOrdemServicoResponse(
                ordem.getId(),
                ordem.getStatus(),
                etapas.stream().map(this::paraEtapa).toList());
    }

    public EtapaOrdemServicoResponse paraEtapa(EtapaOrdemServico etapa) {
        return new EtapaOrdemServicoResponse(
                etapa.getId(),
                etapa.getOrdem(),
                etapa.getNome(),
                etapa.getTipoServico().getId(),
                etapa.getTipoServico().getNome(),
                etapa.getStatus(),
                etapa.getPrazoEstimadoHoras(),
                etapa.getRecurso() == null ? null : etapa.getRecurso().getId());
    }

    public ModeloWorkflowResponse paraModelo(ModeloWorkflow modelo) {
        return new ModeloWorkflowResponse(
                modelo.getId(),
                modelo.getNome(),
                modelo.getDescricao(),
                modelo.getAtivo(),
                modelo.getEtapas().stream()
                        .map(e -> new ModeloWorkflowResponse.Etapa(
                                e.getId(),
                                e.getOrdem(),
                                e.getNome(),
                                e.getTipoServico().getId(),
                                e.getTipoServico().getNome(),
                                e.getPrazoEstimadoHoras()))
                        .toList());
    }
}
