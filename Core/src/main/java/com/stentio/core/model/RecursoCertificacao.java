package com.stentio.core.model;

import com.stentio.core.exception.CertificacaoInvalidaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * US-024: o comprovante de que o profissional pode traduzir de um idioma para outro.
 *
 * <p>Substitui o {@code verificado} global da US-023 na decisão de elegibilidade. Quem
 * atende três pares precisa de três certificações: saber inglês não diz nada sobre alemão,
 * e era isso que o booleano único deixava passar.
 *
 * <p>Uma linha por par e por recurso (RN.2). O reenvio reaproveita a mesma linha em vez de
 * criar outra, então o histórico de documentos não se acumula — o que vale é o atual.
 */
@Entity
@Table(name = "recursos_certificacoes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recursos_certificacoes_par",
                columnNames = {"recurso_id", "idioma_origem_id", "idioma_destino_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecursoCertificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false, updatable = false)
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_origem_id", nullable = false, updatable = false)
    private Idioma idiomaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_destino_id", nullable = false, updatable = false)
    private Idioma idiomaDestino;

    @Column(name = "arquivo_nome", nullable = false, length = 255)
    private String arquivoNome;

    @Column(name = "arquivo_caminho", nullable = false, length = 500)
    private String arquivoCaminho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCertificacao status;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    @Column(name = "analisado_em")
    private Instant analisadoEm;

    /** E-mail de quem analisou, no mesmo formato de HistoricoEtapa.realizadoPor. */
    @Column(name = "analisado_por", length = 150)
    private String analisadoPor;

    @Column(name = "motivo_rejeicao", length = 1000)
    private String motivoRejeicao;

    public RecursoCertificacao(Recurso recurso, Idioma idiomaOrigem, Idioma idiomaDestino,
                               String arquivoNome, String arquivoCaminho, Instant agora) {
        this.recurso = recurso;
        this.idiomaOrigem = idiomaOrigem;
        this.idiomaDestino = idiomaDestino;
        this.arquivoNome = arquivoNome;
        this.arquivoCaminho = arquivoCaminho;
        this.status = StatusCertificacao.PENDENTE;
        this.enviadoEm = agora;
    }

    /**
     * RN.2: o reenvio troca o documento e manda para a fila de novo.
     *
     * <p>Limpa a análise anterior de propósito: o motivo da rejeição passada, exibido ao
     * lado de um documento novo, faria o profissional achar que a recusa continua valendo.
     *
     * @return o caminho do arquivo substituído, para quem chamou removê-lo do disco
     */
    public String reenviar(String novoNome, String novoCaminho, Instant agora) {
        if (status == StatusCertificacao.APROVADA) {
            throw new CertificacaoInvalidaException(
                    "Esta certificação já foi aprovada e não pode ser substituída");
        }
        String caminhoAnterior = this.arquivoCaminho;
        this.arquivoNome = novoNome;
        this.arquivoCaminho = novoCaminho;
        this.status = StatusCertificacao.PENDENTE;
        this.enviadoEm = agora;
        this.analisadoEm = null;
        this.analisadoPor = null;
        this.motivoRejeicao = null;
        return caminhoAnterior;
    }

    /** RN.4: só o que está pendente é analisado. */
    public void aprovar(String quemAnalisou, Instant agora) {
        exigirPendente();
        this.status = StatusCertificacao.APROVADA;
        this.analisadoEm = agora;
        this.analisadoPor = quemAnalisou;
        this.motivoRejeicao = null;
    }

    public void rejeitar(String motivo, String quemAnalisou, Instant agora) {
        exigirPendente();
        this.status = StatusCertificacao.REJEITADA;
        this.analisadoEm = agora;
        this.analisadoPor = quemAnalisou;
        this.motivoRejeicao = motivo;
    }

    public boolean pertenceA(UUID recursoId) {
        return recurso != null && recurso.getId().equals(recursoId);
    }

    private void exigirPendente() {
        if (status != StatusCertificacao.PENDENTE) {
            throw new CertificacaoInvalidaException(
                    "Esta certificação já foi analisada e está como " + status);
        }
    }
}
