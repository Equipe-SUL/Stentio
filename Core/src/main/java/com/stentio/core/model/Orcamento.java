package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orcamentos", uniqueConstraints =
        @UniqueConstraint(name = "uk_orcamentos_token_hash", columnNames = "token_hash"))
@Getter
@Setter
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrcamento status = StatusOrcamento.RASCUNHO;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_expiracao")
    private Instant dataExpiracao;

    // O UUID secreto fica apenas no link enviado ao cliente; o banco guarda SHA-256.
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_utilizado_em")
    private Instant tokenUtilizadoEm;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<OrcamentoItem> itens = new ArrayList<>();

    public void adicionarItem(OrcamentoItem item) {
        item.setOrcamento(this);
        item.setOrdem(itens.size());
        itens.add(item);
    }
}
