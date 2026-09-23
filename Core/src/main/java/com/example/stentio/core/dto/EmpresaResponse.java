package com.example.stentio.core.dto;

import com.example.stentio.core.model.ConfiguracaoEmpresa;

public record EmpresaResponse(

        String nome,
        String cnpj,
        String endereco,
        String telefone,
        boolean logoDisponivel

) {

    public EmpresaResponse(ConfiguracaoEmpresa empresa) {
        this(
                empresa.getNome(),
                empresa.getCnpj(),
                empresa.getEndereco(),
                empresa.getTelefone(),
                empresa.getLogo() != null
                        && empresa.getLogo().length > 0
        );
    }
}