package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * US-011: uma etapa da produção de uma ordem de serviço.
 *
 * <p>O tipo de serviço é chave para o cadastro de tipos_servico, não um enum: o cliente
 * administra esse cadastro pela tela, e um enum congelaria a lista no código.
 */
@Entity
@Table(name = "etapas_os",
        uniqueConstraints = @UniqueConstraint(name = "uk_etapas_os_ordem", columnNames = {"os_id", "ordem"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtapaOrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEtapaOrdemServico status;

    @Column(name = "prazo_estimado_horas", nullable = false)
    private Integer prazoEstimadoHoras;

    /**
     * Quem vai executar a etapa. Fica nulo até a US-012 alocar; aqui serve só para a RN.3,
     * que proíbe remontar uma sequência que já tem gente trabalhando nela.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone default now() not null")
    private Instant criadoEm;

    public EtapaOrdemServico(OrdemServico ordemServico, int ordem, String nome,
                             TipoServico tipoServico, int prazoEstimadoHoras) {
        SequenciaEtapas.validarPrazo(prazoEstimadoHoras);
        this.ordemServico = ordemServico;
        this.ordem = ordem;
        this.nome = normalizar(nome);
        this.tipoServico = tipoServico;
        this.prazoEstimadoHoras = prazoEstimadoHoras;
        this.status = SequenciaEtapas.statusInicial(ordem);
    }

    public boolean alocada() {
        return recurso != null;
    }

    private static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new com.stentio.core.exception.EtapasInvalidasException("Toda etapa precisa de um nome");
        }
        return nome.strip();
    }
}
