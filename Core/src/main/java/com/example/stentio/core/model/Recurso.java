package com.example.stentio.core.model;

import com.example.stentio.core.exception.RecursoInvalidoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "recursos",
        uniqueConstraints = @UniqueConstraint(name = "uk_recursos_email", columnNames = "email")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToMany
    @JoinTable(
            name = "recursos_tipos_servico",
            joinColumns = @JoinColumn(name = "recurso_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_servico_id")
    )
    private Set<TipoServico> tiposServico = new HashSet<>();

    @OneToMany(mappedBy = "recurso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecursoPreco> precos = new ArrayList<>();

    public Recurso(String nome, String email, String telefone) {
        atualizarDados(nome, email, telefone);
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void atualizarDados(String nome, String email, String telefone) {
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.telefone = telefone == null || telefone.isBlank() ? null : telefone.trim();
    }

    public void definirTiposServico(Collection<TipoServico> novosTipos) {
        if (novosTipos == null || novosTipos.isEmpty()) {
            throw new RecursoInvalidoException("Informe ao menos um tipo de serviço");
        }
        tiposServico.clear();
        tiposServico.addAll(novosTipos);
    }

    // Sincroniza em vez de apagar e recriar: o Hibernate executa INSERTs antes de DELETEs,
    // e recriar o mesmo par violaria a constraint única antes de o antigo ser removido.
    public void definirPrecos(List<NovoPreco> novosPrecos) {
        if (novosPrecos == null || novosPrecos.isEmpty()) {
            throw new RecursoInvalidoException("Informe ao menos um par de idioma e valor");
        }

        Map<ChavePreco, NovoPreco> novosPorChave = indexarSemRepeticao(novosPrecos);

        precos.removeIf(preco -> !novosPorChave.containsKey(preco.chave()));

        novosPorChave.forEach((chave, novo) -> precos.stream()
                .filter(existente -> existente.chave().equals(chave))
                .findFirst()
                .ifPresentOrElse(
                        existente -> existente.alterarValor(novo.valor()),
                        () -> precos.add(new RecursoPreco(this, novo))
                ));
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    // Coleções expostas somente para leitura, para que mudanças passem pelas regras acima.
    public Set<TipoServico> getTiposServico() {
        return Collections.unmodifiableSet(tiposServico);
    }

    public List<RecursoPreco> getPrecos() {
        return Collections.unmodifiableList(precos);
    }

    private static Map<ChavePreco, NovoPreco> indexarSemRepeticao(List<NovoPreco> novosPrecos) {
        Map<ChavePreco, NovoPreco> porChave = new LinkedHashMap<>();
        for (NovoPreco novo : novosPrecos) {
            if (porChave.putIfAbsent(novo.chave(), novo) != null) {
                throw new RecursoInvalidoException("Par de idiomas repetido para a mesma unidade de cobrança");
            }
        }
        return porChave;
    }

    public record NovoPreco(Idioma idiomaOrigem, Idioma idiomaDestino, UnidadeCobranca unidade, BigDecimal valor) {

        public NovoPreco {
            Objects.requireNonNull(idiomaOrigem, "idiomaOrigem");
            Objects.requireNonNull(idiomaDestino, "idiomaDestino");
            Objects.requireNonNull(unidade, "unidade");
            Objects.requireNonNull(valor, "valor");

            if (idiomaOrigem.getId().equals(idiomaDestino.getId())) {
                throw new RecursoInvalidoException("Idioma de origem e destino devem ser diferentes");
            }
            if (valor.signum() <= 0) {
                throw new RecursoInvalidoException("O valor deve ser maior que zero");
            }
        }

        ChavePreco chave() {
            return new ChavePreco(idiomaOrigem.getId(), idiomaDestino.getId(), unidade);
        }
    }

    record ChavePreco(UUID idiomaOrigemId, UUID idiomaDestinoId, UnidadeCobranca unidade) {
    }
}
