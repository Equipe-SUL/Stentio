package com.stentio.core.controller;

import com.stentio.core.dto.OrcamentoCriacaoRequest;
import com.stentio.core.dto.OrcamentoDetalheResponse;
import com.stentio.core.dto.OrcamentoItensRequest;
import com.stentio.core.dto.OrcamentoPaginaResponse;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.service.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(OrcamentoController.BASE_PATH)
@Tag(name = "Orçamentos", description = "Gestão de orçamentos vinculados a solicitações")
public class OrcamentoController {

    static final String BASE_PATH = "/api/v1/orcamentos";

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping
    @Operation(summary = "Cria um novo orçamento em elaboração vinculado a uma solicitação")
    public ResponseEntity<OrcamentoDetalheResponse> criar(@Valid @RequestBody OrcamentoCriacaoRequest request) {
        OrcamentoDetalheResponse criado = orcamentoService.criar(request);
        URI localizacao = URI.create(BASE_PATH + "/" + criado.id());
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Substitui os itens e serviços do orçamento e recalcula os totais")
    public ResponseEntity<OrcamentoDetalheResponse> substituirItens(
            @PathVariable UUID id,
            @Valid @RequestBody OrcamentoItensRequest request) {
        OrcamentoDetalheResponse atualizado = orcamentoService.substituirItens(id, request);
        URI localizacao = URI.create(BASE_PATH + "/" + atualizado.id());
        return ResponseEntity.created(localizacao).body(atualizado);
    }

    @GetMapping
    @Operation(summary = "Lista orçamentos paginados com filtros opcionais")
    public ResponseEntity<OrcamentoPaginaResponse> listar(
            @RequestParam(required = false) StatusOrcamento status,
            @RequestParam(name = "cliente_id", required = false) UUID clienteId,
            @RequestParam(name = "solicitacao_id", required = false) UUID solicitacaoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(name = "tamanho_pagina", defaultValue = "10") int tamanhoPagina) {
        return ResponseEntity.ok(orcamentoService.listar(status, clienteId, solicitacaoId, pagina, tamanhoPagina));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o detalhe de um orçamento com sua hierarquia completa de itens e serviços")
    public ResponseEntity<OrcamentoDetalheResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(orcamentoService.buscarPorId(id));
    }
}
