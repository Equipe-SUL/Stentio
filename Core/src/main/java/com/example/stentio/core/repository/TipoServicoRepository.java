package com.example.stentio.core.repository;

import com.example.stentio.core.model.TipoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoServicoRepository extends JpaRepository<TipoServico, UUID> {


    Page<TipoServico> findByNomeContainingIgnoreCase(String nome, Pageable pageable);


    boolean existsByNome(String nome);


    boolean existsByNomeAndIdNot(String nome, UUID id);
}