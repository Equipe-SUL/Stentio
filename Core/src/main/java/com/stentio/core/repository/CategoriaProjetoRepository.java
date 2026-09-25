package com.stentio.core.repository;

import com.stentio.core.model.CategoriaProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoriaProjetoRepository extends JpaRepository<CategoriaProjeto, UUID> {
    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);

    Page<CategoriaProjeto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);


}
