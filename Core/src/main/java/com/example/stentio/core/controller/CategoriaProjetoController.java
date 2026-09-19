package com.example.stentio.core.controller;

import com.example.stentio.core.dto.CategoriaProjetoRequest;
import com.example.stentio.core.dto.CategoriaProjetoResponse;
import com.example.stentio.core.service.CategoriaProjetoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias-projeto")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaProjetoController {

    private final CategoriaProjetoService categoriaProjetoService;

    public CategoriaProjetoController(CategoriaProjetoService categoriaProjetoService) {
        this.categoriaProjetoService = categoriaProjetoService;
    }

    @GetMapping
    public ResponseEntity<Page<CategoriaProjetoResponse>> listar(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<CategoriaProjetoResponse> pagina = categoriaProjetoService.listar(nome, pageable);
        return ResponseEntity.ok(pagina);
    }

    @PostMapping
    public ResponseEntity<CategoriaProjetoResponse> criar(@Valid @RequestBody CategoriaProjetoRequest request) {
        CategoriaProjetoResponse response = categoriaProjetoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        categoriaProjetoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}