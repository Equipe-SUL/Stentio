package com.stentio.core.repository;

import com.stentio.core.model.RecursoCertificacao;
import com.stentio.core.model.StatusCertificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecursoCertificacaoRepository extends JpaRepository<RecursoCertificacao, UUID> {

    /**
     * RN.2: a certificação daquele par, se já existir. O reenvio reaproveita esta linha.
     */
    Optional<RecursoCertificacao> findByRecursoIdAndIdiomaOrigemIdAndIdiomaDestinoId(
            UUID recursoId, UUID idiomaOrigemId, UUID idiomaDestinoId);

    /**
     * CA.1 e CA.5: as certificações do profissional, com os idiomas já carregados.
     *
     * <p>Com open-in-view desligado, ler o nome do idioma depois da transação lançaria
     * LazyInitializationException; e sem o fetch seriam duas consultas por linha.
     */
    @Query("""
            select c from RecursoCertificacao c
              join fetch c.idiomaOrigem
              join fetch c.idiomaDestino
             where c.recurso.id = :recursoId
             order by c.enviadoEm desc
            """)
    List<RecursoCertificacao> listarDoRecurso(@Param("recursoId") UUID recursoId);

    /** CA.2: a fila do gestor, mais antigas primeiro — quem esperou mais é analisado antes. */
    @Query(value = """
            select c from RecursoCertificacao c
              join fetch c.recurso
              join fetch c.idiomaOrigem
              join fetch c.idiomaDestino
             where (:status is null or c.status = :status)
            """,
            countQuery = """
            select count(c) from RecursoCertificacao c
             where (:status is null or c.status = :status)
            """)
    Page<RecursoCertificacao> listarParaAnalise(@Param("status") StatusCertificacao status,
                                                Pageable paginacao);

    /** CA.3 e CA.4: este recurso pode receber uma etapa deste par? */
    boolean existsByRecursoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndStatus(
            UUID recursoId, UUID idiomaOrigemId, UUID idiomaDestinoId, StatusCertificacao status);

    /** O documento, com tudo o que o download precisa conferir antes de entregar. */
    @Query("""
            select c from RecursoCertificacao c
              join fetch c.recurso
              join fetch c.idiomaOrigem
              join fetch c.idiomaDestino
             where c.id = :id
            """)
    Optional<RecursoCertificacao> buscarComRecurso(@Param("id") UUID id);
}
