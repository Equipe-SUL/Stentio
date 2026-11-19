package com.stentio.core.repository;

import com.stentio.core.model.LogAuditoria;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * US-019 RN.1: repositório append-only.
 *
 * <p>Estende {@link Repository}, e não {@code JpaRepository}, para que {@code delete},
 * {@code deleteAll} e afins nem existam: o que não está na interface não pode ser chamado
 * por engano em nenhum service.
 */
public interface LogAuditoriaRepository extends Repository<LogAuditoria, UUID> {

    LogAuditoria save(LogAuditoria log);

    Page<LogAuditoria> findAll(Specification<LogAuditoria> filtros, Pageable paginacao);
}
