package com.stentio.core.repository;

import com.stentio.core.model.Idioma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdiomaRepository extends JpaRepository<Idioma, UUID> {
    boolean existsByCodigoIsoIgnoreCase(String codigoIso);

    // Dois idiomas com o mesmo nome ficam indistinguíveis nos formulários do cliente e do
    // recurso: um escolhe um, o outro cadastra o outro, e o recurso nunca fica elegível.
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);
    
    //bussca idiomas se nome tem o texto informado, ignorando maiúsculas/minúsculas,
    Page<Idioma> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    
    // filtra idiomas pelo status (ativos/inativos), útil para dropdowns de recursos
    Page<Idioma> findByAtivo(Boolean ativo, Pageable pageable);

    List<Idioma> findByAtivoTrueOrderByNomeAsc();
    
    // combina busca por nome com filtro de status
    Page<Idioma> findByNomeContainingIgnoreCaseAndAtivo(String nome, Boolean ativo, Pageable pageable);
    
    // verifica se o codigo id ja existe em outro registro
    boolean existsByCodigoIsoIgnoreCaseAndIdNot(String codigoIso, UUID id);
}
