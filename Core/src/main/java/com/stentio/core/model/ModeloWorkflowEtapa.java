package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// US-011: uma etapa dentro de um modelo de workflow. É o molde que vira EtapaOrdemServico.
@Entity
@Table(name = "modelos_workflow_etapas",
        uniqueConstraints = @UniqueConstraint(name = "uk_modelos_workflow_etapas_ordem",
                columnNames = {"modelo_id", "ordem"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloWorkflowEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false, updatable = false)
    private ModeloWorkflow modelo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private Integer ordem;

    @Column(name = "prazo_estimado_horas", nullable = false)
    private Integer prazoEstimadoHoras;

    public ModeloWorkflowEtapa(ModeloWorkflow modelo, int ordem, String nome,
                               TipoServico tipoServico, int prazoEstimadoHoras) {
        SequenciaEtapas.validarPrazo(prazoEstimadoHoras);
        if (nome == null || nome.isBlank()) {
            throw new com.stentio.core.exception.EtapasInvalidasException("Toda etapa precisa de um nome");
        }
        this.modelo = modelo;
        this.ordem = ordem;
        this.nome = nome.strip();
        this.tipoServico = tipoServico;
        this.prazoEstimadoHoras = prazoEstimadoHoras;
    }
}
