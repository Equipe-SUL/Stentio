package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico",
        uniqueConstraints = @UniqueConstraint(name = "uk_ordens_servico_orcamento", columnNames = "orcamento_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false, updatable = false)
    private Solicitacao solicitacao;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, updatable = false)
    private Orcamento orcamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemServico status = StatusOrdemServico.PENDENTE_ALOCACAO;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone default now() not null")
    private Instant criadoEm;

    public OrdemServico(Orcamento orcamento) {
        this.orcamento = orcamento;
        this.solicitacao = orcamento.getSolicitacao();
    }
}
