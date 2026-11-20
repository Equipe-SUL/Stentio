package com.stentio.core.repository;

import com.stentio.core.dto.DashboardEtapaPendenciaProjection;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.StatusEtapaOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
     * US-013 CA.3 e RN.3: o aceite, em uma instrução só.
     *
     * <p>A condição de estado vai no próprio UPDATE porque é ela que resolve o Cenário 3.
     * Ler, conferir em Java e depois gravar abriria uma janela entre a leitura e a escrita
     * em que os dois candidatos veriam DISPARADA e os dois gravariam — e o segundo
     * sobrescreveria o primeiro em silêncio, sem ninguém receber erro.
     *
     * <p>O banco resolve a disputa: quem chega depois encontra a linha fora de DISPARADA e
     * atualiza zero linhas. Zero é o 409.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update EtapaOrdemServico e
               set e.status = :aceita,
                   e.recurso = :recurso,
                   e.aceitoEm = :agora
             where e.id = :etapaId
               and e.status = :disparada
               and e.recurso is null
            """)
    int aceitarDisparada(@Param("etapaId") UUID etapaId,
                         @Param("recurso") Recurso recurso,
                         @Param("agora") Instant agora,
                         @Param("aceita") StatusEtapaOrdemServico aceita,
                         @Param("disparada") StatusEtapaOrdemServico disparada);

    /**
     * O aceite de alocação nominal também confere o dono no UPDATE. A leitura feita pelo
     * serviço pode ficar obsoleta se o gestor realocar antes desta gravação.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update EtapaOrdemServico e
               set e.status = :aceita,
                   e.aceitoEm = :agora
             where e.id = :etapaId
               and e.status = :alocado
               and e.recurso = :recurso
            """)
    int aceitarAlocacaoDireta(@Param("etapaId") UUID etapaId,
                              @Param("recurso") Recurso recurso,
                              @Param("agora") Instant agora,
                              @Param("aceita") StatusEtapaOrdemServico aceita,
                              @Param("alocado") StatusEtapaOrdemServico alocado);

    /**
     * US-013: a etapa com o que o aceite precisa conferir — par de idiomas e tipo de
     * serviço. Sem o id da OS de propósito: o profissional chega pelo convite, que tem o
     * id da etapa e mais nada.
     */
    @Query("""
            select e from EtapaOrdemServico e
              join fetch e.tipoServico
              join fetch e.ordemServico o
              join fetch o.solicitacao s
              join fetch s.idiomaOrigem
              join fetch s.idiomaDestino
              left join fetch e.recurso
             where e.id = :etapaId
            """)
    Optional<EtapaOrdemServico> buscarParaAceite(@Param("etapaId") UUID etapaId);

    @Query("""
            select e.status from EtapaOrdemServico e
             where e.ordemServico.id = :ordemServicoId
               and e.ordem = :ordem
            """)
    Optional<StatusEtapaOrdemServico> buscarStatusPorOrdem(@Param("ordemServicoId") UUID ordemServicoId,
                                                           @Param("ordem") int ordem);

    Optional<EtapaOrdemServico> findByOrdemServicoAndOrdem(OrdemServico ordemServico, int ordem);

    /**
     * US-013 CA.4: as etapas abertas que este profissional pode assumir.
     *
     * <p>É a consulta da US-012 ao contrário. Lá, dada a etapa, quem pode executá-la; aqui,
     * dado o profissional, quais etapas ele pode pegar. As duas competências são as mesmas
     * — par de idiomas em recursos_precos e especialidade em recursos_tipos_servico —, e o
     * exists mantém ativo e verificado no mesmo lugar: um cadastro ainda não verificado
     * simplesmente não casa, em vez de depender de uma segunda conferência no serviço.
     */
    @Query("""
            select distinct e from EtapaOrdemServico e
              join fetch e.tipoServico ts
              join fetch e.ordemServico o
              join fetch o.solicitacao s
              join fetch s.idiomaOrigem origem
              join fetch s.idiomaDestino destino
             where e.status = :disparada
               and exists (
                   select 1 from Recurso r
                     join r.tiposServico t
                     join r.precos p
                    where r.id = :recursoId
                      and r.ativo = true
                      and r.verificado = true
                      and t.id = ts.id
                      and p.idiomaOrigem.id = origem.id
                      and p.idiomaDestino.id = destino.id)
             order by e.dataLimiteEntrega asc nulls last, e.ordem asc
            """)
    List<EtapaOrdemServico> listarDisponiveisPara(@Param("recursoId") UUID recursoId,
                                                  @Param("disparada") StatusEtapaOrdemServico disparada);

    /**
     * US-023 CA.5: as etapas alocadas a um profissional — a tela "Minhas demandas".
     *
     * <p>Desce até a solicitação porque o par de idiomas mora lá, e o profissional precisa
     * saber de que língua para qual antes de aceitar. Mais raso que isso obrigaria a uma
     * consulta por linha, e com open-in-view desligado nem isso funcionaria.
     *
     * <p>Mais urgente primeiro: a ordenação por data limite é a pergunta que quem executa
     * faz ao abrir a tela.
     */
    @Query("""
            select e from EtapaOrdemServico e
              join fetch e.tipoServico
              join fetch e.ordemServico o
              join fetch o.solicitacao s
              join fetch s.idiomaOrigem
              join fetch s.idiomaDestino
             where e.recurso.id = :recursoId
             order by e.dataLimiteEntrega asc nulls last, e.ordem asc
            """)
    List<EtapaOrdemServico> listarPorRecurso(@Param("recursoId") UUID recursoId);

    /**
     * US-012: a etapa com tudo o que a alocação precisa conferir. O par de idiomas da RN.1
     * mora na solicitação da OS, não na etapa, então a consulta desce até lá — com
     * open-in-view desligado, buscar depois lançaria LazyInitializationException.
     *
     * <p>Filtra por OS e etapa juntas: assim um id de etapa de outra ordem de serviço não
     * é alocado por engano através da rota errada.
     */
    @Query("""
            select e from EtapaOrdemServico e
              join fetch e.tipoServico
              join fetch e.ordemServico o
              join fetch o.solicitacao s
              join fetch s.idiomaOrigem
              join fetch s.idiomaDestino
             where e.id = :etapaId
               and o.id = :ordemServicoId
            """)
    Optional<EtapaOrdemServico> buscarParaAlocacao(@Param("ordemServicoId") UUID ordemServicoId,
                                                   @Param("etapaId") UUID etapaId);

    /**
     * Conta etapas alocadas cujo prazo explícito já venceu. Até a US-014 registrar a
     * entrega do arquivo final, etapas vencidas são consideradas não entregues.
     */
    long countByDataLimiteEntregaBeforeAndRecursoIsNotNull(Instant dataLimiteEntrega);

    /**
     * Lista apenas os campos necessários às pendências do dashboard, sem carregar etapas,
     * recursos ou ordens de serviço como entidades.
     */
    @Query("""
            select new com.stentio.core.dto.DashboardEtapaPendenciaProjection(
                e.id, e.nome, e.dataLimiteEntrega, os.id, os.criadoEm)
              from EtapaOrdemServico e
              join e.ordemServico os
             where e.dataLimiteEntrega < :agora
               and e.recurso is not null
            """)
    List<DashboardEtapaPendenciaProjection> buscarPendenciasDashboard(@Param("agora") Instant agora);

    /**
     * Apaga a sequência antiga antes de gravar a nova. É delete em massa de propósito: se
     * fosse pela coleção da entidade, o Hibernate emitiria os INSERT antes dos DELETE e a
     * constraint de (os_id, ordem) estouraria ao reordenar as etapas.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EtapaOrdemServico e where e.ordemServico.id = :ordemServicoId")
    void apagarPorOrdemServico(@Param("ordemServicoId") UUID ordemServicoId);
}
