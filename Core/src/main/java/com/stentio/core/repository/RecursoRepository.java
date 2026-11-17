package com.stentio.core.repository;

import com.stentio.core.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecursoRepository extends JpaRepository<Recurso, UUID>, JpaSpecificationExecutor<Recurso> {

    boolean existsByEmail(String email);

    // US-023: uma conta de acesso tem no máximo um perfil profissional.
    boolean existsByUsuarioId(UUID usuarioId);

    /** US-023: o perfil de quem está autenticado, resolvido pelo id do token. */
    @Query("""
            select distinct r from Recurso r
              left join fetch r.tiposServico
              left join fetch r.precos
             where r.usuarioId = :usuarioId
            """)
    Optional<Recurso> buscarPorUsuario(@Param("usuarioId") UUID usuarioId);

    boolean existsByEmailAndIdNot(String email, UUID id);

    /**
     * US-012 CA.2: quem está apto a executar uma etapa. As duas competências vêm de
     * cadastros que já existem — o par de idiomas de recursos_precos e a especialidade de
     * recursos_tipos_servico —, então a RN.1 e a RN.2 são a mesma consulta.
     *
     * <p>O distinct é necessário porque um recurso pode ter vários preços para o mesmo par
     * (um por unidade de cobrança) e apareceria repetido.
     */
    @Query("""
            select distinct r from Recurso r
              join r.tiposServico t
              join r.precos p
             where r.ativo = true
               and t.id = :tipoServicoId
               and p.idiomaOrigem.id = :idiomaOrigemId
               and p.idiomaDestino.id = :idiomaDestinoId
             order by r.nome asc
            """)
    List<Recurso> buscarDisponiveis(@Param("idiomaOrigemId") UUID idiomaOrigemId,
                                    @Param("idiomaDestinoId") UUID idiomaDestinoId,
                                    @Param("tipoServicoId") UUID tipoServicoId);

    /** Traz as competências junto: a validação da alocação precisa das duas coleções. */
    @Query("""
            select distinct r from Recurso r
              left join fetch r.tiposServico
              left join fetch r.precos
             where r.id = :id
            """)
    Optional<Recurso> buscarComCompetencias(@Param("id") UUID id);
}
