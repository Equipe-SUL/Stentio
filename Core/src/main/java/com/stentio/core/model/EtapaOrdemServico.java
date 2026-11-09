package com.stentio.core.model;

import com.stentio.core.exception.AlocacaoInvalidaException;
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

    /** Quem vai executar a etapa. Nulo enquanto o gestor não alocou (US-012). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    /** US-012 CA.3: até quando o profissional tem para entregar. Nulo enquanto não alocada. */
    @Column(name = "data_limite_entrega")
    private Instant dataLimiteEntrega;

    /** Quando a alocação aconteceu. Serve ao histórico e a responder "desde quando". */
    @Column(name = "alocado_em")
    private Instant alocadoEm;

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

    /** US-012: o que a API mostra, somando o fluxo com a alocação. */
    public SituacaoEtapa situacao() {
        return SituacaoEtapa.de(this);
    }

    /**
     * US-012 CA.4. Realocar sobrescreve: trocar o profissional é uma alocação nova, e quem
     * decide se podia trocar é a RN.3, não este método.
     */
    public void alocar(Recurso recurso, Instant dataLimiteEntrega, Instant agora) {
        if (recurso == null) {
            throw new AlocacaoInvalidaException("Informe o recurso a alocar");
        }
        if (!recurso.isAtivo()) {
            throw new AlocacaoInvalidaException(
                    "O recurso " + recurso.getNome() + " está inativo e não pode receber etapas");
        }
        if (dataLimiteEntrega == null || !dataLimiteEntrega.isAfter(agora)) {
            throw new AlocacaoInvalidaException("A data limite de entrega deve estar no futuro");
        }
        this.recurso = recurso;
        this.dataLimiteEntrega = dataLimiteEntrega;
        this.alocadoEm = agora;
    }

    /**
     * US-012 RN.3: a partir daqui o recurso já começou e a troca deixa de ser livre.
     *
     * <p>Hoje nenhum status existente significa "em andamento" — a US-013 (aceite do
     * recurso) e a US-014 (entrega) é que vão criá-los. Quando criarem, basta esta linha
     * mudar: o service e o controller não sabem o que é "iniciada".
     */
    public boolean iniciada() {
        return false;
    }

    private static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new com.stentio.core.exception.EtapasInvalidasException("Toda etapa precisa de um nome");
        }
        return nome.strip();
    }
}
