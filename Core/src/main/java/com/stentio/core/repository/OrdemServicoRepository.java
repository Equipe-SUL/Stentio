package com.stentio.core.repository;

import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID>, JpaSpecificationExecutor<OrdemServico> {
    long countByStatus(StatusOrdemServico status);

    /** US-015: um status por consulta não serve mais — "em andamento" virou três. */
    long countByStatusIn(java.util.Collection<StatusOrdemServico> status);

    Optional<OrdemServico> findByOrcamentoId(UUID orcamentoId);

    /**
     * US-015: a OS com o cliente do orçamento já carregado.
     *
     * <p>O destinatário do e-mail final mora em orcamento.cliente (RN.1). Com
     * open-in-view desligado, buscar depois lançaria LazyInitializationException.
     */
    @Query("""
            select os from OrdemServico os
              join fetch os.orcamento o
              join fetch o.cliente
             where os.id = :id
            """)
    Optional<OrdemServico> buscarComCliente(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select os from OrdemServico os
              join fetch os.orcamento o
              join fetch o.cliente
             where os.id = :id
            """)
    Optional<OrdemServico> buscarComClienteParaAtualizar(@Param("id") UUID id);
}
