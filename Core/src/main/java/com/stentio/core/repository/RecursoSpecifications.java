package com.stentio.core.repository;

import com.stentio.core.dto.RecursoFiltro;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.RecursoPreco;
import com.stentio.core.model.TipoServico;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Filtros usam EXISTS em vez de JOIN: um recurso com vários preços não aparece repetido
// na listagem e a paginação continua contando recursos, não linhas de preço.
public final class RecursoSpecifications {

    private RecursoSpecifications() {
    }

    public static Specification<Recurso> filtrar(RecursoFiltro filtro) {
        List<Specification<Recurso>> filtros = new ArrayList<>();

        if (filtro.ativo() != null) {
            filtros.add(comStatusAtivo(filtro.ativo()));
        }
        if (filtro.tipoServicoId() != null) {
            filtros.add(ofereceTipoServico(filtro.tipoServicoId()));
        }
        if (filtro.idiomaOrigemId() != null || filtro.idiomaDestinoId() != null) {
            filtros.add(atendeParDeIdiomas(filtro.idiomaOrigemId(), filtro.idiomaDestinoId()));
        }

        return Specification.allOf(filtros);
    }

    private static Specification<Recurso> comStatusAtivo(boolean ativo) {
        return (recurso, query, cb) -> cb.equal(recurso.get("ativo"), ativo);
    }

    private static Specification<Recurso> ofereceTipoServico(UUID tipoServicoId) {
        return (recurso, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Recurso> mesmoRecurso = subquery.correlate(recurso);
            Join<Recurso, TipoServico> tipos = mesmoRecurso.join("tiposServico");

            subquery.select(tipos.get("id"))
                    .where(cb.equal(tipos.get("id"), tipoServicoId));
            return cb.exists(subquery);
        };
    }

    // Origem e destino são checados na MESMA linha de preço: quem atende EN→PT e ES→FR não atende EN→FR.
    private static Specification<Recurso> atendeParDeIdiomas(UUID idiomaOrigemId, UUID idiomaDestinoId) {
        return (recurso, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<RecursoPreco> preco = subquery.from(RecursoPreco.class);

            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(preco.get("recurso"), recurso));
            if (idiomaOrigemId != null) {
                condicoes.add(cb.equal(preco.get("idiomaOrigem").get("id"), idiomaOrigemId));
            }
            if (idiomaDestinoId != null) {
                condicoes.add(cb.equal(preco.get("idiomaDestino").get("id"), idiomaDestinoId));
            }

            subquery.select(preco.get("id")).where(condicoes.toArray(Predicate[]::new));
            return cb.exists(subquery);
        };
    }
}
