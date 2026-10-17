package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.math.BigDecimal;
import java.util.UUID;

// Valores e descrições são uma fotografia do orçamento: mudar o catálogo depois não altera o que foi orçado.
@Entity
@Table(name = "orcamento_item_servico",
        indexes = @Index(name = "idx_orcamento_item_servico_item", columnList = "orcamento_item_id"))
@Check(name = "ck_orcamento_item_servico_valores", constraints = "quantidade > 0 AND valor_unitario > 0")
@Getter
@Setter
public class OrcamentoItemServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OrcamentoItem item;

    // Nome do tipo de serviço do catálogo (ex.: "Tradução"), guardado como texto.
    @Column(name = "tipo_servico", nullable = false, length = 150)
    private String tipoServico;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadeCobranca unidade;

    @Column(name = "valor_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;
}
