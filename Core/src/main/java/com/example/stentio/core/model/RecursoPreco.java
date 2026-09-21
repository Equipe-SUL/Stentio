package com.example.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "recursos_precos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recursos_precos_par_unidade",
                columnNames = {"recurso_id", "idioma_origem_id", "idioma_destino_id", "unidade"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecursoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_origem_id", nullable = false)
    private Idioma idiomaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_destino_id", nullable = false)
    private Idioma idiomaDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadeCobranca unidade;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal valor;

    // Package-private: preços só nascem pelo Recurso, que é a raiz do agregado.
    RecursoPreco(Recurso recurso, Recurso.NovoPreco dados) {
        this.recurso = recurso;
        this.idiomaOrigem = dados.idiomaOrigem();
        this.idiomaDestino = dados.idiomaDestino();
        this.unidade = dados.unidade();
        this.valor = dados.valor();
    }

    void alterarValor(BigDecimal novoValor) {
        this.valor = novoValor;
    }

    Recurso.ChavePreco chave() {
        return new Recurso.ChavePreco(idiomaOrigem.getId(), idiomaDestino.getId(), unidade);
    }
}
