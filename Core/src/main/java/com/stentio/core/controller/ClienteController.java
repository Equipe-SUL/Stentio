package com.stentio.core.controller;

import com.stentio.core.dto.ClienteFiltro;
import com.stentio.core.dto.ClienteRequest;
import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.exception.ClienteInvalidoException;
import com.stentio.core.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(ClienteController.BASE_PATH)
public class ClienteController {

    public static final String BASE_PATH = "/api/v1/clientes";

    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of(
            "nomeEmpresa", "nomeRepresentante", "emailRepresentante", "cpfCnpj", "dataCadastro", "dataModificacao"
    );

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@RequestBody @Valid ClienteRequest request) {
        ClienteResponse criado = clienteService.criar(request);
        URI localizacao = URI.create(BASE_PATH + "/" + criado.id());
        return ResponseEntity.created(localizacao).body(criado);
    }

    @GetMapping
    public PagedModel<ClienteResponse> listar(
            ClienteFiltro filtro,
            @PageableDefault(size = 20, sort = "nomeEmpresa") Pageable paginacao) {
        validarOrdenacao(paginacao.getSort());
        return new PagedModel<>(clienteService.listar(filtro, paginacao));
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }

    @GetMapping("/cpf-cnpj/{cpfCnpj}")
    public ClienteResponse buscarPorCpfCnpj(@PathVariable String cpfCnpj) {
        return clienteService.buscarPorCpfCnpj(cpfCnpj);
    }

    @GetMapping("/empresa/{empresaId}")
    public PagedModel<ClienteResponse> listarPorEmpresa(
            @PathVariable UUID empresaId,
            @PageableDefault(size = 20, sort = "nomeEmpresa") Pageable paginacao) {
        validarOrdenacao(paginacao.getSort());
        return new PagedModel<>(clienteService.listarPorEmpresa(empresaId, paginacao));
    }

    @PutMapping("/{id}")
    public ClienteResponse editar(@PathVariable UUID id, @RequestBody @Valid ClienteRequest request) {
        return clienteService.editar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private static void validarOrdenacao(Sort ordenacao) {
        ordenacao.forEach(ordem -> {
            if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
                throw new ClienteInvalidoException("Não é possível ordenar por '" + ordem.getProperty() + "'");
            }
        });
    }
}
