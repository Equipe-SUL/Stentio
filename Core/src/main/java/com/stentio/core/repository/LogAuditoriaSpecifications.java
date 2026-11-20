package com.stentio.core.repository;

import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.model.LogAuditoria;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// US-019 CA.3: cada filtro é uma peça independente; só entram na consulta os que vieram preenchidos.
public final class LogAuditoriaSpecifications {

    private static final char ESCAPE = '\\';

    private LogAuditoriaSpecifications() {
    }

    public static Specification<LogAuditoria> comFiltros(LogAuditoriaFiltro filtro) {
        List<Specification<LogAuditoria>> filtros = new ArrayList<>();

        if (filtro.entidade() != null) {
            filtros.add(daEntidade(filtro.entidade()));
        }
        if (filtro.entidadeId() != null) {
            filtros.add(doId(filtro.entidadeId()));
        }
        if (filtro.usuarioId() != null) {
            filtros.add(doUsuario(filtro.usuarioId()));
        }
        if (filtro.dataInicio() != null) {
            filtros.add(aPartirDe(filtro.dataInicio()));
        }
        if (filtro.dataFim() != null) {
            filtros.add(ate(filtro.dataFim()));
        }
        if (filtro.busca() != null && !filtro.busca().isBlank()) {
            filtros.add(contendo(filtro.busca().strip()));
        }

        return Specification.allOf(filtros);
    }

    private static Specification<LogAuditoria> daEntidade(EntidadeAuditada entidade) {
        // A coluna guarda o nome do enum como texto (ver LogAuditoria).
        return (log, query, cb) -> cb.equal(log.get("entidade"), entidade.name());
    }

    private static Specification<LogAuditoria> doId(UUID entidadeId) {
        return (log, query, cb) -> cb.equal(log.get("entidadeId"), entidadeId);
    }

    private static Specification<LogAuditoria> doUsuario(UUID usuarioId) {
        return (log, query, cb) -> cb.equal(log.get("usuarioId"), usuarioId);
    }

    private static Specification<LogAuditoria> aPartirDe(Instant inicio) {
        return (log, query, cb) -> cb.greaterThanOrEqualTo(log.get("dataHora"), inicio);
    }

    // Inclusivo: data_fim=…T23:59:59Z precisa trazer o que aconteceu nesse segundo.
    private static Specification<LogAuditoria> ate(Instant fim) {
        return (log, query, cb) -> cb.lessThanOrEqualTo(log.get("dataHora"), fim);
    }

    /**
     * Busca pelo código que o administrador vê nas telas (está na referência legível, ex.:
     * "ORC-2026-A1B2C3") ou por um trecho do UUID. Sem diferenciar maiúsculas, e com
     * {@code %} e {@code _} escapados para valerem como texto.
     */
    private static Specification<LogAuditoria> contendo(String busca) {
        String padrao = "%" + escapar(busca.toLowerCase(Locale.ROOT)) + "%";
        return (log, query, cb) -> cb.or(
                cb.like(cb.lower(log.get("referenciaLegivel")), padrao, ESCAPE),
                // cast() do Hibernate vira CAST em SQL; o as() do JPA só troca o tipo em Java e o
                // Postgres receberia lower(uuid). O texto de um UUID já sai em minúsculas.
                cb.like(((JpaExpression<?>) log.get("entidadeId")).cast(String.class), padrao, ESCAPE));
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
