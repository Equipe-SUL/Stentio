package com.stentio.core.model;

import com.stentio.core.exception.SolicitacaoInvalidaException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_projeto_id", nullable = false)
    private CategoriaProjeto categoriaProjeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_origem_id", nullable = false)
    private Idioma idiomaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idioma_destino_id", nullable = false)
    private Idioma idiomaDestino;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSolicitacao status;

    @Column(name = "data_recebimento", nullable = false, updatable = false)
    private LocalDateTime dataRecebimento;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArquivoSolicitacao> arquivos = new ArrayList<>();

    public Solicitacao(Cliente cliente,
                       CategoriaProjeto categoriaProjeto,
                       Idioma idiomaOrigem,
                       Idioma idiomaDestino,
                       String descricao) {
        // RN.1: os vínculos são obrigatórios. O service já resolveu os ids em entidades;
        // aqui a entidade garante que ninguém a construa pela metade por outro caminho.
        this.cliente = Objects.requireNonNull(cliente, "Cliente é obrigatório");
        this.categoriaProjeto = Objects.requireNonNull(categoriaProjeto, "Categoria do projeto é obrigatória");
        this.idiomaOrigem = Objects.requireNonNull(idiomaOrigem, "Idioma de origem é obrigatório");
        this.idiomaDestino = Objects.requireNonNull(idiomaDestino, "Idioma de destino é obrigatório");

        // RN.2. Compara por id quando já existe; antes de persistir, dois idiomas só são
        // o mesmo se forem a mesma instância (evita tratar dois ids nulos como iguais).
        boolean mesmoIdioma = idiomaOrigem == idiomaDestino
                || (idiomaOrigem.getId() != null && idiomaOrigem.getId().equals(idiomaDestino.getId()));
        if (mesmoIdioma) {
            throw new SolicitacaoInvalidaException("Os idiomas de origem e destino não devem ser iguais");
        }

        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
        // CA.4: o status nunca vem da requisição.
        this.status = StatusSolicitacao.PENDENTE_ORCAMENTO;
        this.dataRecebimento = LocalDateTime.now();
    }

    public void iniciarOrcamento() {
        if (status != StatusSolicitacao.PENDENTE_ORCAMENTO) {
            throw new SolicitacaoInvalidaException("A solicitação não está pendente de orçamento");
        }
        status = StatusSolicitacao.AGUARDANDO_APROVACAO;
    }

    public void atualizarStatusDoOrcamento(StatusOrcamento statusOrcamento) {
        status = switch (statusOrcamento) {
            case EM_ELABORACAO, AGUARDANDO_APROVACAO -> StatusSolicitacao.AGUARDANDO_APROVACAO;
            case REJEITADO -> StatusSolicitacao.ORCAMENTO_REJEITADO;
            case APROVADO -> StatusSolicitacao.EM_EXECUCAO;
        };
    }

    // RN.3: um ou mais arquivos. Cada chamada anexa um arquivo já gravado no storage.
    public void anexarArquivo(ArquivoArmazenado dados) {
        arquivos.add(new ArquivoSolicitacao(this, Objects.requireNonNull(dados)));
    }

    // Somente leitura: anexar passa obrigatoriamente pelo método acima.
    public List<ArquivoSolicitacao> getArquivos() {
        return Collections.unmodifiableList(arquivos);
    }
}
