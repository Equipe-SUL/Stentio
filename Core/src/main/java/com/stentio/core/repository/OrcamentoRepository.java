package com.stentio.core.repository;

import com.stentio.core.model.Orcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {
    Optional<Orcamento> findByTokenHash(String tokenHash);

    // Trava a linha do orçamento, sem join de coleção: compatível com PostgreSQL.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orcamento o where o.tokenHash = :hash")
    Optional<Orcamento> buscarPorTokenComLock(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orcamento o where o.id = :id")
    Optional<Orcamento> buscarPorIdComLock(@Param("id") UUID id);
}
