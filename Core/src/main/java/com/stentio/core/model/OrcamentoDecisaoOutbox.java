package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orcamento_decisao_outbox", indexes =
        @Index(name = "idx_orcamento_outbox_pendente", columnList = "publicado_em,proxima_tentativa_em,criado_em"))
@Getter
@Setter
public class OrcamentoDecisaoOutbox {
    // O mesmo ID acompanha todas as tentativas e permite deduplicação no consumidor.
    @Id
    private UUID id;

    @Column(name = "orcamento_id", nullable = false, updatable = false)
    private UUID orcamentoId;

    @Column(name = "routing_key", nullable = false, updatable = false, length = 100)
    private String routingKey;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(nullable = false)
    private int tentativas;
}
