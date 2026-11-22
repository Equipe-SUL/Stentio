package com.stentio.core.repository;

import com.stentio.core.model.CertificacaoEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CertificacaoEmailOutboxRepository extends JpaRepository<CertificacaoEmailOutbox, UUID> {

    /** Uma pendente por vez, travada para esta transação. */
    @Query(value = """
            select * from certificacao_email_outbox
             where enviado_em is null
               and proxima_tentativa_em <= :agora
             order by criado_em, id
             limit 1
             for update skip locked
            """, nativeQuery = true)
    Optional<CertificacaoEmailOutbox> buscarPendenteComLock(@Param("agora") Instant agora);
}
