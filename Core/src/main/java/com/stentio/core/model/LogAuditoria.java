package com.stentio.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * US-019 CA.1 / RN.1: um registro de transição de status, append-only.
 *
 * <p>A imutabilidade vale em três camadas: não há setters, toda coluna é
 * {@code updatable = false} e o {@link Immutable} faz o Hibernate ignorar qualquer UPDATE
 * nesta entidade. A API, por sua vez, só expõe leitura.
 *
 * <p>Não há chave estrangeira para a entidade auditada nem para o usuário, de propósito:
 * <ul>
 *   <li>as etapas de uma OS são apagadas e recriadas quando a sequência é remontada
 *       (US-011), e o log precisa sobreviver a isso;</li>
 *   <li>o usuário vive no Auth, outro banco lógico. Nome e perfil são gravados como
 *       <i>snapshot</i> do JWT, para o log continuar dizendo quem fez mesmo que a pessoa
 *       troque de nome ou de perfil depois.</li>
 * </ul>
 * Pelo mesmo motivo a {@code referencia_legivel} é gravada pronta, e não montada na leitura.
 *
 * <p>Os status são texto, e não enum: a mesma coluna guarda status de orçamento, de OS e
 * de etapa. Também evita o CHECK que o Hibernate cria para colunas enum; os valores
 * aceitos são mantidos nas migrações Flyway.
 */
@Entity
@Immutable
@Table(name = "log_auditoria", indexes = {
        @Index(name = "ix_log_auditoria_entidade", columnList = "entidade, entidade_id"),
        @Index(name = "ix_log_auditoria_data_hora", columnList = "data_hora"),
        @Index(name = "ix_log_auditoria_usuario", columnList = "usuario_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogAuditoria {

    static final int TAMANHO_MAXIMO_OBSERVACAO = 2500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Texto puro, exposto como enum pelo getter. Para qualquer atributo de tipo enum — com
    // @Enumerated ou com AttributeConverter — o Hibernate gera um CHECK com os valores atuais.
    // As migrações Flyway atualizam os valores permitidos quando um enum ganha um novo valor.
    // Um valor novo incompatível faria o INSERT desfazer a operação na mesma transação.
    @Getter(AccessLevel.NONE)
    @Column(nullable = false, updatable = false, length = 30)
    private String entidade;

    @Column(name = "entidade_id", nullable = false, updatable = false)
    private UUID entidadeId;

    @Column(name = "referencia_legivel", nullable = false, updatable = false, length = 200)
    private String referenciaLegivel;

    /** Nulo quando o registro marca o nascimento da entidade (ex.: OS criada na aprovação). */
    @Column(name = "status_anterior", updatable = false, length = 40)
    private String statusAnterior;

    @Column(name = "status_novo", nullable = false, updatable = false, length = 40)
    private String statusNovo;

    /** Nulo para atores sem conta (cliente pelo link, sistema) e para tokens antigos sem a claim. */
    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "usuario_nome", nullable = false, updatable = false, length = 150)
    private String usuarioNome;

    @Column(name = "usuario_perfil", nullable = false, updatable = false, length = 40)
    private String usuarioPerfil;

    // Texto puro pelo mesmo motivo da coluna entidade.
    @Getter(AccessLevel.NONE)
    @Column(name = "origem_acao", nullable = false, updatable = false, length = 30)
    private String origemAcao;

    // Cabe o motivo de rejeição do orçamento (até 2000 caracteres) mais o texto fixo em volta.
    @Column(updatable = false, length = TAMANHO_MAXIMO_OBSERVACAO)
    private String observacao;

    /** Instant é gravado em UTC pelo driver: o front converte para o fuso de quem lê. */
    @Column(name = "data_hora", nullable = false, updatable = false)
    private Instant dataHora;

    public LogAuditoria(EntidadeAuditada entidade, UUID entidadeId, String referenciaLegivel,
                        String statusAnterior, String statusNovo,
                        UUID usuarioId, String usuarioNome, String usuarioPerfil,
                        OrigemAcao origemAcao, String observacao, Instant dataHora) {
        this.entidade = Objects.requireNonNull(entidade, "entidade").name();
        this.entidadeId = Objects.requireNonNull(entidadeId, "entidadeId");
        this.referenciaLegivel = Objects.requireNonNull(referenciaLegivel, "referenciaLegivel");
        this.statusAnterior = statusAnterior;
        this.statusNovo = Objects.requireNonNull(statusNovo, "statusNovo");
        this.usuarioId = usuarioId;
        this.usuarioNome = Objects.requireNonNull(usuarioNome, "usuarioNome");
        this.usuarioPerfil = Objects.requireNonNull(usuarioPerfil, "usuarioPerfil");
        this.origemAcao = Objects.requireNonNull(origemAcao, "origemAcao").name();
        // O log é gravado na transação da mudança de status: estourar a coluna desfaria a
        // própria mudança. Cortar o excesso é melhor do que perder a operação do usuário.
        this.observacao = observacao != null && observacao.length() > TAMANHO_MAXIMO_OBSERVACAO
                ? observacao.substring(0, TAMANHO_MAXIMO_OBSERVACAO)
                : observacao;
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora");
    }

    public EntidadeAuditada getEntidade() {
        return EntidadeAuditada.valueOf(entidade);
    }

    public OrigemAcao getOrigemAcao() {
        return OrigemAcao.valueOf(origemAcao);
    }
}
