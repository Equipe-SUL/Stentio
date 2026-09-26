package com.stentio.core.repository;

import com.stentio.core.model.Idioma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdiomaRepository extends JpaRepository<Idioma, UUID> {
    boolean existsByCodigoIso(String codigoIso);
    
    //bussca idiomas se nome tem o texto informado, ignorando maiúsculas/minúsculas,
    Page<Idioma> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    
    // filtra idiomas pelo status (ativos/inativos), útil para dropdowns de recursos
    Page<Idioma> findByAtivo(Boolean ativo, Pageable pageable);
    
    // combina busca por nome com filtro de status
    Page<Idioma> findByNomeContainingIgnoreCaseAndAtivo(String nome, Boolean ativo, Pageable pageable);
    
    // verifica se o codigo id ja existe em outro registro
    boolean existsByCodigoIsoAndIdNot(String codigoIso, UUID id);
}
