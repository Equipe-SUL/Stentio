package com.example.stentio.core.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaPatchRequest(

        @Size(
                max = 150,
                message = "Nome deve possuir no máximo 150 caracteres"
        )
        String nome,

        String cnpj,

        @Size(
                max = 300,
                message = "Endereço deve possuir no máximo 300 caracteres"
        )
        String endereco,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\s]{8,20}$",
                message = "Telefone inválido"
        )
        String telefone

) {
}