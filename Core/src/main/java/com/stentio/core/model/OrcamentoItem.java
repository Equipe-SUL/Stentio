package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

// Valores e descrições são uma fotografia do orçamento enviado ao cliente.
@Entity
@Table(name = "orcamento_itens", indexes =
        @Index(name = "idx_orcamento_itens_orcamento", columnList = "orcamento_id"))
@Getter
@Setter
public class OrcamentoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "tipo_servico", nullable = false, length = 150)
    private String tipoServico;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    @Column(nullable = false, length = 50)
    private String unidade;

    @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 6)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
}
