package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orcamentos",
        uniqueConstraints = @UniqueConstraint(name = "uk_orcamentos_token_hash", columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_orcamentos_status", columnList = "status"),
                @Index(name = "idx_orcamentos_cliente", columnList = "cliente_id"),
                @Index(name = "idx_orcamentos_solicitacao", columnList = "solicitacao_id")
        })
@Getter
@Setter
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Controle otimista do JPA (US-13/14). Não é a versão de negócio exibida ao usuário.
    @Version
    private long versao;

    // US-11: versão de negócio do orçamento; nesta US apenas inicia em 1.
    // O default no banco permite que o ddl-auto adicione a coluna em tabelas que já têm linhas.
    @Column(name = "numero_versao", nullable = false, columnDefinition = "integer default 1 not null")
    private int numeroVersao = 1;

    // TODO(US-11): tornar NOT NULL no banco quando não houver mais orçamentos antigos sem solicitação
    // (os dados demonstrativos da US-14 foram criados antes deste vínculo). O service já exige o campo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrcamento status = StatusOrcamento.EM_ELABORACAO;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "data_expiracao")
    private Instant dataExpiracao;

    // O UUID secreto fica apenas no link enviado ao cliente; o banco guarda SHA-256.
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_utilizado_em")
    private Instant tokenUtilizadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone default now() not null")
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false,
            columnDefinition = "timestamp(6) with time zone default now() not null")
    private Instant atualizadoEm;

    // Cada item é um par de idiomas com a sua lista de serviços (US-11).
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<OrcamentoItem> itens = new ArrayList<>();

    public void adicionarItem(OrcamentoItem item) {
        item.setOrcamento(this);
        itens.add(item);
    }
}
