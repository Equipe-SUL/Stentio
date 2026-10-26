package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// US-11: um item do orçamento é um par de idiomas; os serviços precificados ficam em OrcamentoItemServico.
// Tabela nova (orcamento_item): a antiga orcamento_itens tinha colunas de serviço NOT NULL que não cabem aqui.
@Entity
@Table(name = "orcamento_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_orcamento_item_ordem", columnNames = {"orcamento_id", "ordem"}),
        indexes = @Index(name = "idx_orcamento_item_orcamento", columnList = "orcamento_id"))
@Check(name = "ck_orcamento_item_idiomas_diferentes", constraints = "idioma_origem_id <> idioma_destino_id")
@Getter
@Setter
public class OrcamentoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Orcamento orcamento;

    @Column(nullable = false)
    private int ordem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_origem_id", nullable = false)
    private Idioma idiomaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_destino_id", nullable = false)
    private Idioma idiomaDestino;

    @Column(name = "subtotal_item", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalItem = BigDecimal.ZERO;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrcamentoItemServico> servicos = new ArrayList<>();

    public void adicionarServico(OrcamentoItemServico servico) {
        servico.setItem(this);
        servicos.add(servico);
    }
}
