package com.stentio.core.dto;

import com.stentio.core.validation.CpfOuCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClienteRequest(

        @NotBlank(message = "Nome da empresa é obrigatório")
        @Size(max = 150, message = "Nome da empresa deve ter no máximo 150 caracteres")
        String nomeEmpresa,

        @NotBlank(message = "Nome do representante é obrigatório")
        @Size(max = 100, message = "Nome do representante deve ter no máximo 100 caracteres")
        String nomeRepresentante,

        @NotBlank(message = "E-mail do representante é obrigatório")
        @Email(message = "E-mail do representante inválido")
        @Size(max = 150, message = "E-mail do representante deve ter no máximo 150 caracteres")
        String emailRepresentante,

        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{8,20}$", message = "Telefone inválido")
        String telefone,

        @NotNull(message = "ID da empresa é obrigatório")
        UUID empresaId,

        @NotBlank(message = "CPF ou CNPJ é obrigatório")
        @CpfOuCnpj(message = "CPF ou CNPJ inválido")
        String cpfCnpj
) {

    public ClienteRequest {
        nomeEmpresa = semEspacosNasPontas(nomeEmpresa);
        nomeRepresentante = semEspacosNasPontas(nomeRepresentante);
        emailRepresentante = semEspacosNasPontas(emailRepresentante);
        telefone = semEspacosNasPontas(telefone);
        cpfCnpj = semEspacosNasPontas(cpfCnpj);
    }

    private static String semEspacosNasPontas(String valor) {
        return valor == null ? null : valor.strip();
    }
}
