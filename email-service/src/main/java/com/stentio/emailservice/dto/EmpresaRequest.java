package com.stentio.emailservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "CNPJ é obrigatório")
        @Pattern(
                regexp =
                        "^(?:[A-Za-z0-9]{12}\\d{2}"
                                + "|[A-Za-z0-9]{2}\\.[A-Za-z0-9]{3}\\."
                                + "[A-Za-z0-9]{3}/[A-Za-z0-9]{4}-\\d{2})$",
                message = "CNPJ inválido"
        )
        String cnpj,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(max = 300)
        String endereco,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\s]{8,20}$",
                message = "Telefone inválido"
        )
        String telefone

) {

    public EmpresaRequest {
        nome = strip(nome);
        cnpj = strip(cnpj);
        endereco = strip(endereco);
        telefone = strip(telefone);
    }

    private static String strip(String valor) {
        return valor == null ? null : valor.strip();
    }
}
