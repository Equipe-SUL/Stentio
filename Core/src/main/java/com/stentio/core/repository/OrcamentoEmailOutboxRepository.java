package com.stentio.core.repository;

import com.stentio.core.model.OrcamentoEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoEmailOutboxRepository extends JpaRepository<OrcamentoEmailOutbox, UUID> {
    @Query(value = """
            select * from orcamento_email_outbox
            where enviado_em is null and proxima_tentativa_em <= :agora
            order by criado_em, id
            limit 1 for update skip locked
            """, nativeQuery = true)
    Optional<OrcamentoEmailOutbox> buscarPendenteComLock(@Param("agora") Instant agora);
}
