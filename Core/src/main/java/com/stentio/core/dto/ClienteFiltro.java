package com.stentio.core.dto;

public record ClienteFiltro(
        String termo,
        String cpfCnpj,
        String emailRepresentante
) {
}
