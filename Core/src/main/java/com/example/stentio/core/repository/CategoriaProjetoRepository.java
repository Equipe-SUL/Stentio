package com.example.stentio.core.repository;

import com.example.stentio.core.model.CategoriaProjeto;
import com.example.stentio.core.model.Idioma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoriaProjetoRepository extends JpaRepository<CategoriaProjeto, UUID> {
    boolean existsByNome(String nome);

    Page<CategoriaProjeto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);


}
