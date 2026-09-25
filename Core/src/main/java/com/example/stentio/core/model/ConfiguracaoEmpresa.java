package com.example.stentio.core.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(name = "configuracao_empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfiguracaoEmpresa {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 300)
    private String endereco;

    @Column(length = 20)
    private String telefone;

    @Column(columnDefinition = "bytea")
    private byte[] logo;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Column(name = "logo_nome_arquivo", length = 255)
    private String logoNomeArquivo;

    public ConfiguracaoEmpresa(
            String nome,
            String cnpj,
            String endereco,
            String telefone
    ) {
        atualizarDados(nome, cnpj, endereco, telefone);
    }

    public void atualizarDados(
            String nome,
            String cnpj,
            String endereco,
            String telefone
    ) {
        this.nome = nome.strip();
        this.cnpj = normalizarCnpj(cnpj);
        this.endereco = endereco.strip();

        this.telefone =
                telefone == null || telefone.isBlank()
                        ? null
                        : telefone.strip();
    }

    public void atualizarNome(String nome) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome não pode ser vazio"
            );
        }

        this.nome = nome.strip();
    }


    public void atualizarCnpj(String cnpj) {

        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException(
                    "CNPJ não pode ser vazio"
            );
        }

        this.cnpj = normalizarCnpj(cnpj);
    }


    public void atualizarEndereco(String endereco) {

        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException(
                    "Endereço não pode ser vazio"
            );
        }

        this.endereco = endereco.strip();
    }


    public void atualizarTelefone(String telefone) {

        if (telefone == null || telefone.isBlank()) {
            this.telefone = null;
            return;
        }

        this.telefone = telefone.strip();
    }

    public void atualizarLogo(
            byte[] conteudo,
            String contentType,
            String nomeArquivo
    ) {
        this.logo = conteudo.clone();
        this.logoContentType = contentType;
        this.logoNomeArquivo = nomeArquivo;
    }

    public static String normalizarCnpj(String cnpj) {

        if (cnpj == null) {
            return null;
        }

        return cnpj.strip()
                .replace(".", "")
                .replace("/", "")
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }
}