package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * US-024 CA.5: o aviso de rejeição, gravado na transação que rejeitou.
 *
 * <p>Outbox própria porque a NotificacaoEtapaOutbox tem FK obrigatória para etapa, e aqui
 * não há etapa nenhuma — é um aviso sobre o cadastro do profissional.
 *
 * <p>Publicar direto dentro da transação seria mais curto e errado nos dois sentidos: uma
 * falha no broker desfaria a rejeição, e um rollback depois do envio avisaria o
 * profissional de uma recusa que não aconteceu.
 */
@Entity
@Table(name = "certificacao_email_outbox",
        indexes = @Index(name = "ix_certificacao_email_outbox_pendentes",
                columnList = "enviado_em, proxima_tentativa_em"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CertificacaoEmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificacao_id", nullable = false, updatable = false)
    private RecursoCertificacao certificacao;

    @Column(nullable = false, length = 150, updatable = false)
    private String destinatario;

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

    public CertificacaoEmailOutbox(RecursoCertificacao certificacao, String destinatario,
                                   String payload, Instant agora) {
        this.certificacao = certificacao;
        this.destinatario = destinatario;
        this.payload = payload;
        this.criadoEm = agora;
        this.proximaTentativaEm = agora;
    }
}
