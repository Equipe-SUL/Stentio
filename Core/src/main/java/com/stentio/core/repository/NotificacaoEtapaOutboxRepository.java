package com.stentio.core.repository;

import com.stentio.core.model.NotificacaoEtapaOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificacaoEtapaOutboxRepository extends JpaRepository<NotificacaoEtapaOutbox, UUID> {

    /**
     * {@code for update skip locked}: se um dia o Core rodar em mais de uma instância, cada
     * uma pega uma notificação diferente em vez de disputarem o mesmo e enviarem duas vezes.
     */
    @Query(value = """
            select * from notificacao_etapa_outbox
            where enviado_em is null and proxima_tentativa_em <= :agora
            order by criado_em, id
            limit 1 for update skip locked
            """, nativeQuery = true)
    Optional<NotificacaoEtapaOutbox> buscarPendenteComLock(@Param("agora") Instant agora);
}
