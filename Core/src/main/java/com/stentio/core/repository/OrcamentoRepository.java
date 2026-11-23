package com.stentio.core.repository;

import com.stentio.core.dto.DashboardOrcamentoPendenciaProjection;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.StatusOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID>, JpaSpecificationExecutor<Orcamento> {
    Optional<Orcamento> findByTokenHash(String tokenHash);

    long countByStatus(StatusOrcamento status);

    @Query("""
            select new com.stentio.core.dto.DashboardOrcamentoPendenciaProjection(
                o.id, o.status, o.dataExpiracao, o.criadoEm, c.nomeEmpresa)
              from Orcamento o
              join o.cliente c
             where o.status in :statuses
            """)
    List<DashboardOrcamentoPendenciaProjection> buscarPendenciasDashboard(
            @Param("statuses") Collection<StatusOrcamento> statuses);

    boolean existsBySolicitacaoId(UUID solicitacaoId);

    // Trava a linha do orçamento, sem join de coleção: compatível com PostgreSQL.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orcamento o where o.tokenHash = :hash")
    Optional<Orcamento> buscarPorTokenComLock(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orcamento o where o.id = :id")
    Optional<Orcamento> buscarPorIdComLock(@Param("id") UUID id);

    // US-11: detalhe. Busca os itens no mesmo SELECT; os serviços de cada item vêm em lote
    // (default_batch_fetch_size). Dois "join fetch" em listas gerariam MultipleBagFetchException.
    @Query("select distinct o from Orcamento o left join fetch o.itens where o.id = :id")
    Optional<Orcamento> buscarDetalhePorId(@Param("id") UUID id);

    // Consulta pública: carrega uma única coleção (itens) nesta consulta.
    // Arquivos e serviços são inicializados pelo service em consultas separadas,
    // pois três join fetch de listas causariam MultipleBagFetchException.
    @Query("select distinct o from Orcamento o " +
            "left join fetch o.solicitacao s " +
            "left join fetch s.categoriaProjeto " +
            "left join fetch s.idiomaOrigem " +
            "left join fetch s.idiomaDestino " +
            "left join fetch o.itens i " +
            "where o.tokenHash = :hash")
    Optional<Orcamento> buscarPublicoPorTokenHash(@Param("hash") String hash);
}
