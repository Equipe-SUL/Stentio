package com.stentio.core.repository;

import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID>, JpaSpecificationExecutor<OrdemServico> {
    long countByStatus(StatusOrdemServico status);

    Optional<OrdemServico> findByOrcamentoId(UUID orcamentoId);
}
