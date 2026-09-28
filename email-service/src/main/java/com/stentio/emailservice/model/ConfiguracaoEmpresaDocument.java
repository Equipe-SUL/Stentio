package com.stentio.emailservice.model;

import com.stentio.emailservice.exception.RequisicaoInvalidaException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Locale;

// Registro único da empresa, no mesmo padrão do id fixo "default" usado na configuração de SMTP.
@Document("configuracao_empresa")
public class ConfiguracaoEmpresaDocument {

    public static final String ID_UNICO = "default";

    @Id
    private String id;
    private String nome;
    private String cnpj;
    private String endereco;
    private String telefone;
    private byte[] logo;
    private String logoContentType;
    private String logoNomeArquivo;

    protected ConfiguracaoEmpresaDocument() {
    }

    public ConfiguracaoEmpresaDocument(
            String id,
            String nome,
            String cnpj,
            String endereco,
            String telefone
    ) {
        this.id = id;
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
        this.telefone = telefone == null || telefone.isBlank() ? null : telefone.strip();
    }

    public void atualizarNome(String nome) {
        // Preserva a invariante do modelo removido do Core: nome é obrigatório.
        if (nome == null || nome.isBlank()) {
            throw new RequisicaoInvalidaException("Nome não pode ser vazio");
        }
        this.nome = nome.strip();
    }

    public void atualizarCnpj(String cnpj) {
        this.cnpj = normalizarCnpj(cnpj);
    }

    public void atualizarEndereco(String endereco) {
        // Preserva a invariante do modelo removido do Core: endereço é obrigatório.
        if (endereco == null || endereco.isBlank()) {
            throw new RequisicaoInvalidaException("Endereço não pode ser vazio");
        }
        this.endereco = endereco.strip();
    }

    public void atualizarTelefone(String telefone) {
        this.telefone = telefone == null || telefone.isBlank() ? null : telefone.strip();
    }

    public void atualizarLogo(byte[] conteudo, String contentType, String nomeArquivo) {
        this.logo = conteudo.clone();
        this.logoContentType = contentType;
        this.logoNomeArquivo = nomeArquivo;
    }

    public boolean possuiLogo() {
        return logo != null && logo.length > 0;
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

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public String getEndereco() { return endereco; }
    public String getTelefone() { return telefone; }
    public byte[] getLogo() { return logo; }
    public String getLogoContentType() { return logoContentType; }
    public String getLogoNomeArquivo() { return logoNomeArquivo; }
}
