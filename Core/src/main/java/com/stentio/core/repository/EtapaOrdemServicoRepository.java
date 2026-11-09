package com.stentio.core.repository;

import com.stentio.core.model.EtapaOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EtapaOrdemServicoRepository extends JpaRepository<EtapaOrdemServico, UUID> {

    // O recurso entra como left join fetch porque a resposta mostra o nome de quem está
    // alocado: sem isso seria uma consulta por etapa, e com open-in-view desligado nem isso.
    @Query("""
            select e from EtapaOrdemServico e
              join fetch e.tipoServico
              left join fetch e.recurso
             where e.ordemServico.id = :ordemServicoId
             order by e.ordem asc
            """)
    List<EtapaOrdemServico> listarPorOrdemServico(@Param("ordemServicoId") UUID ordemServicoId);

    // RN.3: basta uma etapa alocada para a sequência ficar congelada.
    boolean existsByOrdemServicoIdAndRecursoIsNotNull(UUID ordemServicoId);

    /**
     * Apaga a sequência antiga antes de gravar a nova. É delete em massa de propósito: se
     * fosse pela coleção da entidade, o Hibernate emitiria os INSERT antes dos DELETE e a
     * constraint de (os_id, ordem) estouraria ao reordenar as etapas.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EtapaOrdemServico e where e.ordemServico.id = :ordemServicoId")
    void apagarPorOrdemServico(@Param("ordemServicoId") UUID ordemServicoId);
}
