package com.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orcamento_email_outbox", indexes =
        @Index(name = "idx_orcamento_email_outbox_pendente",
                columnList = "enviado_em,proxima_tentativa_em,criado_em"))
@Getter
@Setter
public class OrcamentoEmailOutbox {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_orcamento_email_outbox_orcamento"))
    private Orcamento orcamento;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(nullable = false)
    private int tentativas;
}
