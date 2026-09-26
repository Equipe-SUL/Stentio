package com.stentio.core.repository;

import com.stentio.core.dto.ClienteFiltro;
import com.stentio.core.model.Cliente;
import com.stentio.core.validation.CpfCnpjUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    public static Specification<Cliente> filtrar(ClienteFiltro filtro) {
        return (root, query, builder) -> {
            if (filtro == null) {
                return builder.conjunction();
            }

            List<Predicate> predicados = new ArrayList<>();

            if (filtro.empresaId() != null) {
                predicados.add(builder.equal(root.get("empresaId"), filtro.empresaId()));
            }

            if (filtro.cpfCnpj() != null && !filtro.cpfCnpj().isBlank()) {
                String digitos = CpfCnpjUtils.somenteDigitos(filtro.cpfCnpj());
                predicados.add(builder.equal(root.get("cpfCnpj"), digitos));
            }

            if (filtro.emailRepresentante() != null && !filtro.emailRepresentante().isBlank()) {
                predicados.add(builder.equal(
                        builder.lower(root.get("emailRepresentante")),
                        filtro.emailRepresentante().trim().toLowerCase()
                ));
            }

            if (filtro.termo() != null && !filtro.termo().isBlank()) {
                String termoLower = "%" + filtro.termo().trim().toLowerCase() + "%";
                Predicate porEmpresa = builder.like(builder.lower(root.get("nomeEmpresa")), termoLower);
                Predicate porRepresentante = builder.like(builder.lower(root.get("nomeRepresentante")), termoLower);
                predicados.add(builder.or(porEmpresa, porRepresentante));
            }

            return builder.and(predicados.toArray(new Predicate[0]));
        };
    }
}
