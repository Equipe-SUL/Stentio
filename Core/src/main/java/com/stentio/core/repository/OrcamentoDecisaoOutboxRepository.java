package com.stentio.core.repository;

import com.stentio.core.model.OrcamentoDecisaoOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoDecisaoOutboxRepository extends JpaRepository<OrcamentoDecisaoOutbox, UUID> {
    // Cada instância pega uma mensagem; linhas já processadas por outra instância são ignoradas.
    @Query(value = """
            select * from orcamento_decisao_outbox
            where publicado_em is null and proxima_tentativa_em <= :agora
            order by criado_em, id
            limit 1 for update skip locked
            """, nativeQuery = true)
    Optional<OrcamentoDecisaoOutbox> buscarPendenteComLock(@Param("agora") Instant agora);
}
