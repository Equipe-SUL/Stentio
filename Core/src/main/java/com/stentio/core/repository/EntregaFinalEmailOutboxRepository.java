package com.stentio.core.repository;

import com.stentio.core.model.EntregaFinalEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EntregaFinalEmailOutboxRepository extends JpaRepository<EntregaFinalEmailOutbox, UUID> {

    /** RN.4 e CA.12: existe envio desta OS ainda sem confirmação do broker? */
    boolean existsByOrdemServicoIdAndEnviadoEmIsNull(UUID ordemServicoId);

    /** CA.9: a aprovação repetida devolve a mensagem que já existe, sem criar outra. */
    Optional<EntregaFinalEmailOutbox> findByIdempotencyKey(String idempotencyKey);

    /**
     * Uma pendente por vez, travada para esta transação. O skip locked deixa duas
     * instâncias do dispatcher trabalharem lado a lado sem pegar a mesma linha.
     */
    @Query(value = """
            select * from entrega_final_email_outbox
             where enviado_em is null
               and proxima_tentativa_em <= :agora
             order by criado_em, id
             limit 1
             for update skip locked
            """, nativeQuery = true)
    Optional<EntregaFinalEmailOutbox> buscarPendenteComLock(@Param("agora") Instant agora);
}
