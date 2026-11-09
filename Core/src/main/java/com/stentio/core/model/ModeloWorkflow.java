package com.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * US-011: sequência de etapas pré-cadastrada que o gestor aplica a uma ordem de serviço
 * em vez de montar tudo na mão. Guarda o prazo estimado de cada etapa, senão a OS nasceria
 * com etapas sem prazo e violaria a RN.2 no mesmo instante em que o modelo fosse aplicado.
 */
@Entity
@Table(name = "modelos_workflow",
        uniqueConstraints = @UniqueConstraint(name = "uk_modelos_workflow_nome", columnNames = "nome"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "modelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ModeloWorkflowEtapa> etapas = new ArrayList<>();

    public ModeloWorkflow(String nome, String descricao) {
        this.nome = exigirNome(nome);
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    public void renomear(String nome, String descricao) {
        this.nome = exigirNome(nome);
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    public void ativar(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Troca a sequência inteira em vez de mexer item a item. Alterar no lugar evita o
     * problema conhecido do Hibernate de executar o INSERT antes do DELETE quando a
     * coleção é esvaziada e repovoada na mesma transação.
     */
    public void definirEtapas(List<ModeloWorkflowEtapa> novas) {
        SequenciaEtapas.validarOrdens(novas.stream().map(ModeloWorkflowEtapa::getOrdem).toList());
        this.etapas.clear();
        this.etapas.addAll(novas);
    }

    public List<ModeloWorkflowEtapa> getEtapas() {
        List<ModeloWorkflowEtapa> ordenadas = new ArrayList<>(etapas);
        ordenadas.sort(Comparator.comparing(ModeloWorkflowEtapa::getOrdem));
        return Collections.unmodifiableList(ordenadas);
    }

    private static String exigirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new com.stentio.core.exception.EtapasInvalidasException("Informe o nome do modelo");
        }
        return nome.strip();
    }
}
