package com.stentio.core.repository;

import com.stentio.core.model.Orcamento;
import com.stentio.core.model.StatusOrcamento;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

// Filtros opcionais da listagem de orçamentos (US-11). Filtro nulo não restringe a consulta.
public final class OrcamentoSpecifications {

    private OrcamentoSpecifications() {
    }

    public static Specification<Orcamento> comFiltros(StatusOrcamento status, UUID clienteId, UUID solicitacaoId) {
        return (root, query, cb) -> cb.and(
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status),
                clienteId == null ? cb.conjunction() : cb.equal(root.get("cliente").get("id"), clienteId),
                solicitacaoId == null ? cb.conjunction() : cb.equal(root.get("solicitacao").get("id"), solicitacaoId)
        );
    }
}
