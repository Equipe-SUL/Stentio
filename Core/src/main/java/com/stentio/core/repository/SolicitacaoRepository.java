package com.stentio.core.repository;

import com.stentio.core.model.Solicitacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitacao s where s.id = :id")
    Optional<Solicitacao> buscarPorIdComLock(@Param("id") UUID id);
}
