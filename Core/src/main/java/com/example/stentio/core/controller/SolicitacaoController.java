package com.example.stentio.core.controller;

import com.example.stentio.core.dto.SolicitacaoRequest;
import com.example.stentio.core.dto.SolicitacaoResponse;
import com.example.stentio.core.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criar(@Valid @RequestBody SolicitacaoRequest request) {
        SolicitacaoResponse response = solicitacaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}