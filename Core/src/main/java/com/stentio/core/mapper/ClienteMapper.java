package com.stentio.core.mapper;

import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteResponse paraResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNomeEmpresa(),
                cliente.getNomeRepresentante(),
                cliente.getEmailRepresentante(),
                cliente.getTelefone(),
                cliente.getEmpresaId(),
                cliente.getCpfCnpj(),
                cliente.getDataCadastro(),
                cliente.getDataModificacao()
        );
    }
}
