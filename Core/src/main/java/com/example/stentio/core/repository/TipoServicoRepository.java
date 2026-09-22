package com.example.stentio.core.repository;

import com.example.stentio.core.model.TipoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoServicoRepository extends JpaRepository<TipoServico, UUID> {
    Page<TipoServico> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // filtra tipos de serviço pelo status (ativos/inativos), útil para dropdowns de recursos
    Page<TipoServico> findByAtivo(Boolean ativo, Pageable pageable);

    // combina busca por nome com filtro de status
    Page<TipoServico> findByNomeContainingIgnoreCaseAndAtivo(String nome, Boolean ativo, Pageable pageable);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);
}