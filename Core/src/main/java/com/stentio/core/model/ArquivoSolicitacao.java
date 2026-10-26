package com.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "arquivos_solicitacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArquivoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;

    // Nome que o usuário enviou, usado só para exibição e download.
    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    // Caminho relativo dentro do diretório de storage; nunca o nome original.
    @Column(name = "caminho", nullable = false, length = 500)
    private String caminho;

    @Column(name = "tipo_conteudo", length = 120)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;

    // Package-private: arquivos só nascem pela Solicitacao, que é a raiz do agregado.
    ArquivoSolicitacao(Solicitacao solicitacao, ArquivoArmazenado dados) {
        this.solicitacao = solicitacao;
        this.nomeOriginal = dados.nomeOriginal();
        this.caminho = dados.caminho();
        this.tipoConteudo = dados.tipoConteudo();
        this.tamanhoBytes = dados.tamanhoBytes();
        this.enviadoEm = LocalDateTime.now();
    }
}
