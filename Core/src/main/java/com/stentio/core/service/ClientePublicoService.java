package com.stentio.core.service;

import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.exception.CpfCnpjJaCadastradoException;
import com.stentio.core.mapper.ClienteMapper;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.validation.CpfCnpjUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientePublicoService {

    private final ClienteRepository clientes;
    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;

    public ClientePublicoService(
            ClienteRepository clientes,
            ClienteService clienteService,
            ClienteMapper clienteMapper) {
        this.clientes = clientes;
        this.clienteService = clienteService;
        this.clienteMapper = clienteMapper;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ClienteResponse buscarOuCriar(SolicitacaoPublicaRequest requisicao) {
        String documento = CpfCnpjUtils.somenteDigitos(requisicao.cpfCnpj());

        return clientes.findByCpfCnpj(documento)
                .map(clienteMapper::paraResponse)
                .orElseGet(() -> criarOuRecuperarConcorrente(requisicao, documento));
    }

    private ClienteResponse criarOuRecuperarConcorrente(SolicitacaoPublicaRequest requisicao, String documento) {
        ClienteRequest dadosCliente = new ClienteRequest(
                requisicao.nomeEmpresa(),
                requisicao.nomeRepresentante(),
                requisicao.emailRepresentante(),
                requisicao.telefone(),
                documento
        );

        try {
            return clienteService.criar(dadosCliente);
        } catch (CpfCnpjJaCadastradoException | DataIntegrityViolationException ex) {
            // ClienteService conclui/rollbacka sua transação antes de chegar aqui; a restrição
            // única do banco decide o vencedor e a nova consulta recupera esse cadastro.
            return clientes.findByCpfCnpj(documento)
                    .map(clienteMapper::paraResponse)
                    .orElseThrow(() -> ex);
        }
    }
}
