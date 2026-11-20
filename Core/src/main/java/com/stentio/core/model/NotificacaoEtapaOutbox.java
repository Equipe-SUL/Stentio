package com.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * US-013 CA.2: uma notificação à espera de ser publicado na fila de e-mail.
 *
 * <p>Mesma mecânica da outbox do orçamento, e pelo mesmo motivo: a linha é gravada na
 * transação do disparo. Se o RabbitMQ estiver fora do ar, o disparo não deixa de
 * acontecer e a notificação não se perde — ele sai quando a fila voltar. Mandar direto da
 * transação daria o contrário nos dois sentidos: e-mail enviado de uma transação que
 * depois falhou, ou disparo desfeito porque o e-mail não saiu.
 */
@Entity
@Table(name = "notificacao_etapa_outbox",
        indexes = @Index(name = "ix_notificacao_etapa_outbox_pendentes",
                columnList = "enviado_em, proxima_tentativa_em"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificacaoEtapaOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_id", nullable = false, updatable = false)
    private EtapaOrdemServico etapa;

    /** Guardado para o log: depois do envio o payload é apagado, e o destino se perderia. */
    @Column(nullable = false, length = 150, updatable = false)
    private String destinatario;

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

    public NotificacaoEtapaOutbox(EtapaOrdemServico etapa, String destinatario, String payload, Instant agora) {
        this.etapa = etapa;
        this.destinatario = destinatario;
        this.payload = payload;
        this.criadoEm = agora;
        this.proximaTentativaEm = agora;
        this.tentativas = 0;
    }
}
