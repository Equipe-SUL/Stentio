package com.stentio.core.repository;

import com.stentio.core.model.ModeloWorkflow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModeloWorkflowRepository extends JpaRepository<ModeloWorkflow, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    // Carrega as etapas junto: a conversão acontece no service, com a sessão já fechada depois.
    @EntityGraph(attributePaths = {"etapas", "etapas.tipoServico"})
    Optional<ModeloWorkflow> findWithEtapasById(UUID id);

    @EntityGraph(attributePaths = {"etapas", "etapas.tipoServico"})
    List<ModeloWorkflow> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = {"etapas", "etapas.tipoServico"})
    List<ModeloWorkflow> findAllByAtivoOrderByNomeAsc(Boolean ativo);
}
