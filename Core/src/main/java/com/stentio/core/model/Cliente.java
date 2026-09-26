package com.stentio.core.model;

import com.stentio.core.validation.CpfCnpjUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "clientes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_clientes_cpf_cnpj", columnNames = "cpf_cnpj")
        },
        indexes = {
                @Index(name = "idx_clientes_empresa_id", columnList = "empresa_id"),
                @Index(name = "idx_clientes_cpf_cnpj", columnList = "cpf_cnpj")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "nome_empresa", nullable = false, length = 150)
    private String nomeEmpresa;

    @Column(name = "nome_representante", nullable = false, length = 100)
    private String nomeRepresentante;

    @Column(name = "email_representante", nullable = false, length = 150)
    private String emailRepresentante;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "cpf_cnpj", nullable = false, length = 14)
    private String cpfCnpj;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_modificacao", nullable = false)
    private LocalDateTime dataModificacao;

    public Cliente(String nomeEmpresa,
                   String nomeRepresentante,
                   String emailRepresentante,
                   String telefone,
                   UUID empresaId,
                   String cpfCnpj) {
        atualizarDados(nomeEmpresa, nomeRepresentante, emailRepresentante, telefone, empresaId, cpfCnpj);
    }

    public void atualizarDados(String nomeEmpresa,
                               String nomeRepresentante,
                               String emailRepresentante,
                               String telefone,
                               UUID empresaId,
                               String cpfCnpj) {
        this.nomeEmpresa = Objects.requireNonNull(nomeEmpresa, "Nome da empresa é obrigatório").trim();
        this.nomeRepresentante = Objects.requireNonNull(nomeRepresentante, "Nome do representante é obrigatório").trim();
        this.emailRepresentante = normalizarEmail(Objects.requireNonNull(emailRepresentante, "E-mail do representante é obrigatório"));
        this.telefone = telefone == null || telefone.isBlank() ? null : telefone.trim();
        this.empresaId = Objects.requireNonNull(empresaId, "ID da empresa é obrigatório");
        this.cpfCnpj = normalizarCpfCnpj(Objects.requireNonNull(cpfCnpj, "CPF ou CNPJ é obrigatório"));
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizarCpfCnpj(String cpfCnpj) {
        return CpfCnpjUtils.somenteDigitos(cpfCnpj);
    }

    @PrePersist
    protected void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (this.dataCadastro == null) {
            this.dataCadastro = agora;
        }
        if (this.dataModificacao == null) {
            this.dataModificacao = agora;
        }
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.dataModificacao = LocalDateTime.now();
    }
}
