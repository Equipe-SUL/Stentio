package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orcamento_historico", indexes =
        @Index(name = "idx_orcamento_historico_orcamento", columnList = "orcamento_id"))
@Getter
@Setter
public class OrcamentoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, updatable = false)
    private Orcamento orcamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, updatable = false, length = 30)
    private StatusOrcamento statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, updatable = false, length = 30)
    private StatusOrcamento statusNovo;

    @Column(updatable = false, length = 2000)
    private String motivo;

    @Column(name = "data_decisao", nullable = false, updatable = false)
    private Instant dataDecisao;

    @Column(nullable = false, updatable = false, length = 30)
    private String origem = "CLIENTE_LINK";
}
