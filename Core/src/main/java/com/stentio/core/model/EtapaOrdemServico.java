package com.stentio.core.model;

import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.AlocacaoNaoPermitidaException;
import com.stentio.core.exception.TransicaoEtapaInvalidaException;
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

    /** US-013: quando o convite aberto saiu. */
    @Column(name = "disparada_em")
    private Instant disparadaEm;

    /** US-013 CA.3: quando o profissional assumiu. */
    @Column(name = "aceito_em")
    private Instant aceitoEm;

    /** US-013 CA.5: quando o arquivo chegou. */
    @Column(name = "entregue_em")
    private Instant entregueEm;

    /**
     * US-013 CA.5: a entrega. Guarda o nome que o profissional enviou e o caminho interno
     * do arquivo — o mesmo par que os anexos da solicitação já usam, porque o nome original
     * é o que o revisor reconhece e o caminho é o que o storage entende.
     */
    @Column(name = "arquivo_entregue_nome", length = 255)
    private String arquivoEntregueNome;

    @Column(name = "arquivo_entregue_caminho", length = 500)
    private String arquivoEntregueCaminho;

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

    /** RN.1: a primeira etapa é livre; as demais dependem da entrega da etapa anterior. */
    public boolean liberada(StatusEtapaOrdemServico statusAnterior) {
        return ordem == SequenciaEtapas.ORDEM_INICIAL
                || statusAnterior == StatusEtapaOrdemServico.ENTREGUE;
    }

    public void liberar() {
        if (status != StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " não está aguardando liberação");
        }
        status = StatusEtapaOrdemServico.PENDENTE;
    }

    /**
     * US-012 CA.4. Realocar sobrescreve: trocar o profissional é uma alocação nova, e quem
     * decide se podia trocar é a RN.3, não este método.
     *
     * <p>US-013: alocar passou a ser um convite nominal, não uma atribuição. O status vai
     * para ALOCADO e fica esperando o aceite — a mesma coisa que a US-012 já mostrava na
     * resposta, agora gravada em vez de deduzida.
     */
    public void alocar(Recurso recurso, Instant dataLimiteEntrega, Instant agora) {
        if (recurso == null) {
            throw new AlocacaoInvalidaException("Informe o recurso a alocar");
        }
        // US-023 RN.5: inativo ou não verificado não recebe etapa. A pergunta é uma só,
        // mas a mensagem diz qual das duas faltou — "não pode receber etapas" sem motivo
        // manda o gestor procurar no lugar errado.
        if (!recurso.elegivelParaAlocacao()) {
            throw new AlocacaoInvalidaException("O recurso " + recurso.getNome()
                    + (recurso.isAtivo()
                            ? " ainda não foi verificado e não pode receber etapas"
                            : " está inativo e não pode receber etapas"));
        }
        if (dataLimiteEntrega == null || !dataLimiteEntrega.isAfter(agora)) {
            throw new AlocacaoInvalidaException("A data limite de entrega deve estar no futuro");
        }
        this.recurso = recurso;
        this.dataLimiteEntrega = dataLimiteEntrega;
        this.alocadoEm = agora;
        this.status = StatusEtapaOrdemServico.ALOCADO;
    }

    /**
     * US-013 CA.1: abre a etapa para todos os elegíveis.
     *
     * <p>Um convite nominal anterior é desfeito de propósito: se o gestor decidiu abrir, a
     * escolha de antes deixou de valer, e manter o recurso ali faria a etapa parecer de
     * alguém que não foi quem aceitou.
     *
     * <p>RN.6: etapa já assumida ou entregue não volta para a fila.
     */
    public void disparar(Instant agora, boolean liberada) {
        if (!liberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " aguarda a entrega da etapa anterior");
        }
        if (status != StatusEtapaOrdemServico.PENDENTE
                && status != StatusEtapaOrdemServico.ALOCADO) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " não está disponível para disparo");
        }
        this.recurso = null;
        this.alocadoEm = null;
        this.disparadaEm = agora;
        this.status = StatusEtapaOrdemServico.DISPARADA;
    }

    /**
     * US-013 CA.3. Só sai de um convite em aberto: quem chega depois do primeiro encontra
     * a etapa fora de DISPARADA e recebe a recusa.
     */
    public void aceitarPor(Recurso quemAceita, Instant agora, boolean liberada) {
        if (status == StatusEtapaOrdemServico.DISPARADA && !liberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " aguarda a entrega da etapa anterior");
        }
        if (status != StatusEtapaOrdemServico.DISPARADA && status != StatusEtapaOrdemServico.ALOCADO) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " não está disponível para aceite");
        }
        this.recurso = quemAceita;
        this.aceitoEm = agora;
        this.status = StatusEtapaOrdemServico.ACEITA;
    }

    /**
     * US-013 RN.5: o profissional devolve um convite nominal, e a etapa volta para a fila.
     *
     * <p>Só vale para o convite nominal. Recusar um convite aberto é simplesmente não
     * aceitar — não há o que devolver, e tirar a etapa de DISPARADA porque alguém não
     * quis a esconderia de quem queria.
     */
    public void recusar(boolean liberada) {
        if (status != StatusEtapaOrdemServico.ALOCADO) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " não tem um convite seu para recusar");
        }
        this.recurso = null;
        this.alocadoEm = null;
        this.dataLimiteEntrega = null;
        this.status = liberada
                ? StatusEtapaOrdemServico.PENDENTE
                : StatusEtapaOrdemServico.AGUARDANDO_ETAPA_ANTERIOR;
    }

    /** US-013 CA.5 e RN.4: só entrega quem assumiu. */
    public void entregar(String nomeDoArquivo, String caminho, Instant agora, boolean liberada) {
        if (status != StatusEtapaOrdemServico.ACEITA) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " precisa estar aceita para receber a entrega");
        }
        if (!liberada) {
            throw new TransicaoEtapaInvalidaException(
                    "A etapa " + nome + " aguarda a entrega da etapa anterior");
        }
        this.arquivoEntregueNome = nomeDoArquivo;
        this.arquivoEntregueCaminho = caminho;
        this.entregueEm = agora;
        this.status = StatusEtapaOrdemServico.ENTREGUE;
    }

    /** US-013 CA.7: a etapa é deste profissional? */
    public boolean pertenceA(Recurso candidato) {
        return recurso != null && candidato != null && recurso.getId().equals(candidato.getId());
    }

    /**
     * US-012 RN.3: a partir daqui o recurso já começou e a troca deixa de ser livre.
     *
     * <p>US-013: o gancho que a US-012 deixou agora responde de verdade. Começar é
     * aceitar — enquanto o convite está só de pé (ALOCADO ou DISPARADA), ninguém
     * trabalhou ainda e o gestor pode corrigir a escolha livremente.
     */
    public boolean iniciada() {
        return status == StatusEtapaOrdemServico.ACEITA || status == StatusEtapaOrdemServico.ENTREGUE;
    }

    /**
     * US-012 RN.3: depois que o profissional começa, trocar quem está no trabalho deixa de
     * ser decisão do gestor. Enquanto a etapa não começou, realocar é livre — corrigir uma
     * escolha errada é o uso normal da tela.
     */
    public void garantirQuePodeTrocarRecurso(boolean administrador) {
        if (iniciada() && !administrador) {
            throw new AlocacaoNaoPermitidaException(
                    "A etapa " + nome + " já está em andamento; trocar o recurso exige perfil administrativo");
        }
    }

    private static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new com.stentio.core.exception.EtapasInvalidasException("Toda etapa precisa de um nome");
        }
        return nome.strip();
    }
}
