package com.stentio.core.controller;

import com.stentio.core.dto.OrdemServicoDetalheResponse;
import com.stentio.core.dto.OrdemServicoPaginaResponse;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.service.OrdemServicoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// US-17: somente leitura. A criação da OS acontece ao consumir a aprovação do orçamento (US-16).
@RestController
@RequestMapping(OrdemServicoController.BASE_PATH)
public class OrdemServicoController {

    static final String BASE_PATH = "/api/v1/ordens-servico";

    private final OrdemServicoConsultaService ordemServicoConsultaService;

    public OrdemServicoController(OrdemServicoConsultaService ordemServicoConsultaService) {
        this.ordemServicoConsultaService = ordemServicoConsultaService;
    }

    // CA.1 e CA.2. Mesma assinatura da listagem de orçamentos: filtros em snake_case e página base 1.
    @GetMapping
    @Operation(summary = "Lista ordens de serviço paginadas, com filtros opcionais de status e cliente (cliente_id ou cliente)")
    public ResponseEntity<OrdemServicoPaginaResponse> listar(
            @RequestParam(required = false) StatusOrdemServico status,
            @RequestParam(name = "cliente_id", required = false) UUID clienteId,
            @RequestParam(name = "cliente", required = false) UUID cliente,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(name = "tamanho_pagina", defaultValue = "10") int tamanhoPagina) {
        UUID filtroCliente = clienteId != null ? clienteId : cliente;
        return ResponseEntity.ok(ordemServicoConsultaService.listar(status, filtroCliente, pagina, tamanhoPagina));
    }

    // CA.3
    @GetMapping("/{id}")
    @Operation(summary = "Consulta o detalhe de uma ordem de serviço com os vínculos de cliente e solicitação")
    public ResponseEntity<OrdemServicoDetalheResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoConsultaService.buscarPorId(id));
    }
}
