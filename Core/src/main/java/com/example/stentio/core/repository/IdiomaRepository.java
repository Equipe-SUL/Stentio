package com.example.stentio.core.repository;

import com.example.stentio.core.model.Idioma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdiomaRepository extends JpaRepository<Idioma, UUID> {
    boolean existsByCodigoIso(String codigoIso);
    //bussca idiomas se nome tem o texto informado, ignorando maiúsculas/minúsculas,
    Page<Idioma> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

}
