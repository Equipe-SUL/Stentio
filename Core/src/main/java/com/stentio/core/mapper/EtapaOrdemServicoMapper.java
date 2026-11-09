package com.stentio.core.mapper;

import com.stentio.core.dto.ModeloWorkflowResponse;
import com.stentio.core.model.ModeloWorkflow;
import org.springframework.stereotype.Component;

// Converte entidade em resposta. Não consulta o banco: quem carrega as entidades é o service.
@Component
public class EtapaOrdemServicoMapper {

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
