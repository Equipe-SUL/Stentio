package com.example.stentio.core.controller;

import com.example.stentio.core.dto.AlteracaoStatusRequest;
import com.example.stentio.core.dto.TipoServicoRequest;
import com.example.stentio.core.dto.TipoServicoResponse;
import com.example.stentio.core.service.TipoServicoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tipos-servico")
public class TipoServicoController {

    private final TipoServicoService tipoServicoService;

    public TipoServicoController(TipoServicoService tipoServicoService) {
        this.tipoServicoService = tipoServicoService;
    }

    @PostMapping
    public ResponseEntity<TipoServicoResponse> criar(@Valid @RequestBody TipoServicoRequest request) {
        TipoServicoResponse response = tipoServicoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TipoServicoResponse>> listar(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<TipoServicoResponse> pagina = tipoServicoService.listar(nome, pageable);
        return ResponseEntity.ok(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoServicoResponse> editar(@PathVariable UUID id, @Valid @RequestBody TipoServicoRequest request) {
        TipoServicoResponse response = tipoServicoService.editar(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TipoServicoResponse> alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlteracaoStatusRequest request) {
        TipoServicoResponse resposta = tipoServicoService.alterarStatus(id, request.ativo());
        return ResponseEntity.ok(resposta);
    }
}