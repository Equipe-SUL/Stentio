package com.stentio.core.model;

import com.stentio.core.exception.TransicaoOrdemServicoInvalidaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico",
        uniqueConstraints = @UniqueConstraint(name = "uk_ordens_servico_orcamento", columnNames = "orcamento_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false, updatable = false)
    private Solicitacao solicitacao;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, updatable = false)
    private Orcamento orcamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemServico status = StatusOrdemServico.PENDENTE_ALOCACAO;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone default now() not null")
    private Instant criadoEm;

    /** US-015: quando o envio ao cliente foi confirmado pelo broker. */
    @Column(name = "entregue_em")
    private Instant entregueEm;

    /** US-015: e-mail do gestor que aprovou, no mesmo formato de HistoricoEtapa.realizadoPor. */
    @Column(name = "entregue_por", length = 150)
    private String entreguePor;

    /** US-015 CA.11: por que a última entrega foi reprovada. Nulo enquanto não houver pendência. */
    @Column(name = "motivo_revisao", length = 1000)
    private String motivoRevisao;

    public OrdemServico(Orcamento orcamento) {
        this.orcamento = orcamento;
        this.solicitacao = orcamento.getSolicitacao();
    }

    /**
     * US-011: definir a sequência de etapas tira a OS da espera e a coloca em planejamento.
     * É idempotente de propósito — remontar as etapas não deve mudar nada no status.
     */
    public void planejar() {
        this.status = StatusOrdemServico.EM_PLANEJAMENTO;
    }

    public void iniciarExecucao() {
        if (status == StatusOrdemServico.EM_PLANEJAMENTO) {
            this.status = StatusOrdemServico.EM_EXECUCAO;
        }
    }

    /**
     * US-014 e US-015 CA.13: a sequência acabou e a OS espera a conferência do gestor.
     *
     * <p>Limpa o motivo da revisão porque chegar aqui significa que a correção pedida foi
     * entregue. Deixar o motivo antigo faria a tela acusar uma pendência já resolvida.
     */
    public void aguardarVerificacao() {
        this.status = StatusOrdemServico.AGUARDANDO_VERIFICACAO;
        this.motivoRevisao = null;
    }

    /**
     * US-015 CA.8: o e-mail ao cliente foi aceito pelo broker e a OS fecha.
     *
     * <p>Chamado pelo dispatcher, depois do ACK — nunca na requisição do gestor. É o que
     * impede uma OS encerrada cuja mensagem nunca saiu.
     */
    public void encerrar(Instant agora, String quemAprovou) {
        if (status != StatusOrdemServico.AGUARDANDO_VERIFICACAO) {
            throw new TransicaoOrdemServicoInvalidaException(
                    "Só uma ordem de serviço aguardando verificação pode ser encerrada");
        }
        this.entregueEm = agora;
        this.entreguePor = quemAprovou;
        this.motivoRevisao = null;
        this.status = StatusOrdemServico.ENCERRADA;
    }

    /**
     * US-015 CA.11: o gestor reprovou e a OS volta para execução até a correção chegar.
     */
    public void reprovarEntrega(String motivo) {
        if (status != StatusOrdemServico.AGUARDANDO_VERIFICACAO) {
            throw new TransicaoOrdemServicoInvalidaException(
                    "Só uma ordem de serviço aguardando verificação pode ser reprovada");
        }
        this.entregueEm = null;
        this.entreguePor = null;
        this.motivoRevisao = motivo;
        this.status = StatusOrdemServico.EM_EXECUCAO;
    }
}
