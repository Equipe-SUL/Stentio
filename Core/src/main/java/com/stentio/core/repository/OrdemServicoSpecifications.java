package com.stentio.core.repository;

import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Cada filtro é uma peça independente; só entram na consulta os que vieram preenchidos.
public final class OrdemServicoSpecifications {

    private OrdemServicoSpecifications() {
    }

    public static Specification<OrdemServico> comFiltros(StatusOrdemServico status, UUID clienteId) {
        List<Specification<OrdemServico>> filtros = new ArrayList<>();

        if (status != null) {
            filtros.add(comStatus(status));
        }
        if (clienteId != null) {
            filtros.add(doCliente(clienteId));
        }

        return Specification.allOf(filtros);
    }

    private static Specification<OrdemServico> comStatus(StatusOrdemServico status) {
        return (ordem, query, cb) -> cb.equal(ordem.get("status"), status);
    }

    // A OS não guarda o cliente: ele vem do orçamento que a originou, onde a coluna é obrigatória.
    // Como são relações ToOne, navegar pelo caminho gera um inner join sem multiplicar linhas.
    private static Specification<OrdemServico> doCliente(UUID clienteId) {
        return (ordem, query, cb) -> cb.equal(ordem.get("orcamento").get("cliente").get("id"), clienteId);
    }
}
