package com.stentio.core.service;

import com.stentio.core.dto.ClienteFiltro;
import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.exception.ClienteInvalidoException;
import com.stentio.core.exception.ClienteNaoEncontradoException;
import com.stentio.core.exception.CpfCnpjJaCadastradoException;
import com.stentio.core.mapper.ClienteMapper;
import com.stentio.core.model.Cliente;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.ClienteSpecifications;
import com.stentio.core.validation.CpfCnpjUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        String cpfCnpjLimpo = CpfCnpjUtils.somenteDigitos(request.cpfCnpj());

        if (clienteRepository.existsByCpfCnpj(cpfCnpjLimpo)) {
            throw new CpfCnpjJaCadastradoException();
        }

        Cliente cliente = new Cliente(
                request.nomeEmpresa(),
                request.nomeRepresentante(),
                request.emailRepresentante(),
                request.telefone(),
                request.empresaId(),
                cpfCnpjLimpo
        );

        return clienteMapper.paraResponse(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse editar(UUID id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        String cpfCnpjLimpo = CpfCnpjUtils.somenteDigitos(request.cpfCnpj());

        if (clienteRepository.existsByCpfCnpjAndIdNot(cpfCnpjLimpo, id)) {
            throw new CpfCnpjJaCadastradoException();
        }

        cliente.atualizarDados(
                request.nomeEmpresa(),
                request.nomeRepresentante(),
                request.emailRepresentante(),
                request.telefone(),
                request.empresaId(),
                cpfCnpjLimpo
        );
        clienteRepository.flush();

        return clienteMapper.paraResponse(cliente);
    }

    @Transactional
    public void excluir(UUID id) {
        Cliente cliente = buscarEntidade(id);
        clienteRepository.delete(cliente);
        clienteRepository.flush();
    }

    public ClienteResponse buscarPorId(UUID id) {
        return clienteMapper.paraResponse(buscarEntidade(id));
    }

    public ClienteResponse buscarPorCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.isBlank()) {
            throw new ClienteInvalidoException("CPF ou CNPJ para busca não pode ser vazio");
        }
        String digitos = CpfCnpjUtils.somenteDigitos(cpfCnpj);
        return clienteRepository.findByCpfCnpj(digitos)
                .map(clienteMapper::paraResponse)
                .orElseThrow(() -> new ClienteNaoEncontradoException("Cliente não encontrado com CPF/CNPJ: " + cpfCnpj));
    }

    public Page<ClienteResponse> listarPorEmpresa(UUID empresaId, Pageable paginacao) {
        if (empresaId == null) {
            throw new ClienteInvalidoException("ID da empresa não pode ser nulo");
        }
        return clienteRepository.findAllByEmpresaId(empresaId, paginacao)
                .map(clienteMapper::paraResponse);
    }

    public Page<ClienteResponse> listar(ClienteFiltro filtro, Pageable paginacao) {
        return clienteRepository.findAll(ClienteSpecifications.filtrar(filtro), paginacao)
                .map(clienteMapper::paraResponse);
    }

    private Cliente buscarEntidade(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNaoEncontradoException(id));
    }
}
