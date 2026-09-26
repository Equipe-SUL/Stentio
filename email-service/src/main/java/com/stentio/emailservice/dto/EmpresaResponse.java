package com.stentio.emailservice.dto;

import com.stentio.emailservice.model.ConfiguracaoEmpresaDocument;

public record EmpresaResponse(

        String nome,
        String cnpj,
        String endereco,
        String telefone,
        boolean logoDisponivel

) {

    public EmpresaResponse(ConfiguracaoEmpresaDocument empresa) {
        this(
                empresa.getNome(),
                empresa.getCnpj(),
                empresa.getEndereco(),
                empresa.getTelefone(),
                empresa.possuiLogo()
        );
    }
}
