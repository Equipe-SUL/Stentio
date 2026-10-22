package com.stentio.core.repository;

import com.stentio.core.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {
    Optional<OrdemServico> findByOrcamentoId(UUID orcamentoId);
}
