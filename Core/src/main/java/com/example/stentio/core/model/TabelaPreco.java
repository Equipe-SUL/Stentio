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
        name = "tabelas_preco",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tabelas_preco_combinacao",
                columnNames = {"tipo_servico_id", "idioma_origem_id", "idioma_destino_id", "unidade"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TabelaPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

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

    public TabelaPreco(TipoServico tipoServico, Idioma idiomaOrigem, Idioma idiomaDestino,
                       UnidadeCobranca unidade, BigDecimal valor) {
        atualizar(tipoServico, idiomaOrigem, idiomaDestino, unidade, valor);
    }

    public void atualizar(TipoServico tipoServico, Idioma idiomaOrigem, Idioma idiomaDestino,
                          UnidadeCobranca unidade, BigDecimal valor) {
        this.tipoServico = tipoServico;
        this.idiomaOrigem = idiomaOrigem;
        this.idiomaDestino = idiomaDestino;
        this.unidade = unidade;
        this.valor = valor;
    }
}
