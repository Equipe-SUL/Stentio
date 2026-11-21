package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * US-015 CA.7: a intenção de enviar o pacote ao cliente, gravada junto com a aprovação.
 *
 * <p>Mesmo padrão da outbox do orçamento: a linha entra na transação que aprova, e só
 * depois alguém tenta publicar. Sem isso daria para ter OS aprovada sem e-mail, ou e-mail
 * de uma aprovação que acabou falhando.
 *
 * <p>A chave de idempotência é única no banco (CA.9): é ela, e não uma checagem em Java,
 * que garante que duas aprovações simultâneas não gerem dois e-mails.
 */
@Entity
@Table(name = "entrega_final_email_outbox",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entrega_final_outbox_idempotency", columnNames = "idempotency_key"),
        indexes = @Index(
                name = "ix_entrega_final_outbox_pendentes", columnList = "enviado_em, proxima_tentativa_em"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntregaFinalEmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String idempotencyKey;

    /** E-mail do gestor que aprovou; vira OrdemServico.entreguePor quando o ACK chega. */
    @Column(name = "aprovado_por", nullable = false, updatable = false, length = 150)
    private String aprovadoPor;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(nullable = false)
    private int tentativas;

    public EntregaFinalEmailOutbox(OrdemServico ordemServico, String idempotencyKey,
                                   String aprovadoPor, String payload, Instant agora) {
        this.ordemServico = ordemServico;
        this.idempotencyKey = idempotencyKey;
        this.aprovadoPor = aprovadoPor;
        this.payload = payload;
        this.criadoEm = agora;
        this.proximaTentativaEm = agora;
    }
}
