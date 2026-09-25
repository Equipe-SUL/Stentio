package com.stentio.core.repository;

import com.stentio.core.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RecursoRepository extends JpaRepository<Recurso, UUID>, JpaSpecificationExecutor<Recurso> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
