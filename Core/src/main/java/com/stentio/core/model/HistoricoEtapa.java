package com.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.UUID;

/**
 * US-012 CA.4: o rastro de cada alocação, espelhando o orcamento_historico que já existe.
 *
 * <p>Tudo é {@code updatable = false}: histórico que pode ser editado não é histórico. Uma
 * correção entra como evento novo, nunca sobrescrevendo o anterior.
 *
 * <p>O autor é gravado como e-mail porque é o que o Core sabe: o JWT emitido pelo Auth traz
 * e-mail e perfil, e nenhum id de usuário atravessa a fronteira entre os dois serviços.
 */
@Entity
@Table(name = "etapas_os_historico",
        indexes = @Index(name = "ix_etapas_os_historico_etapa", columnList = "etapa_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoricoEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_id", nullable = false, updatable = false)
    private EtapaOrdemServico etapa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private EventoEtapa evento;

    /** Nulo na primeira alocação; preenchido quando houve troca, que é o caso interessante. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_anterior_id", updatable = false)
    private Recurso recursoAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_novo_id", nullable = false, updatable = false)
    private Recurso recursoNovo;

    @Column(name = "data_limite_entrega", nullable = false, updatable = false)
    private Instant dataLimiteEntrega;

    @Column(name = "realizado_por", nullable = false, updatable = false, length = 150)
    private String realizadoPor;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private Instant realizadoEm;

    public HistoricoEtapa(EtapaOrdemServico etapa, Recurso recursoAnterior, Recurso recursoNovo,
                          Instant dataLimiteEntrega, String realizadoPor, Instant realizadoEm) {
        this.etapa = etapa;
        this.evento = recursoAnterior == null ? EventoEtapa.ALOCACAO : EventoEtapa.REALOCACAO;
        this.recursoAnterior = recursoAnterior;
        this.recursoNovo = recursoNovo;
        this.dataLimiteEntrega = dataLimiteEntrega;
        this.realizadoPor = realizadoPor;
        this.realizadoEm = realizadoEm;
    }
}
