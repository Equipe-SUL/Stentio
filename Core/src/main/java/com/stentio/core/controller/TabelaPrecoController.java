package com.stentio.core.controller;

import com.stentio.core.dto.TabelaPrecoRequest;
import com.stentio.core.dto.TabelaPrecoResponse;
import com.stentio.core.service.TabelaPrecoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tabelas-preco")
public class TabelaPrecoController {

    private final TabelaPrecoService tabelaPrecoService;

    public TabelaPrecoController(TabelaPrecoService tabelaPrecoService) {
        this.tabelaPrecoService = tabelaPrecoService;
    }

    @PostMapping
    public ResponseEntity<TabelaPrecoResponse> criar(@Valid @RequestBody TabelaPrecoRequest request) {
        TabelaPrecoResponse response = tabelaPrecoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TabelaPrecoResponse>> listar(
            @PageableDefault(sort = {"tipoServico.nome", "idiomaOrigem.nome", "idiomaDestino.nome", "unidade"})
            Pageable pageable) {
        return ResponseEntity.ok(tabelaPrecoService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TabelaPrecoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(tabelaPrecoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TabelaPrecoResponse> editar(@PathVariable UUID id,
                                                      @Valid @RequestBody TabelaPrecoRequest request) {
        return ResponseEntity.ok(tabelaPrecoService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        tabelaPrecoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
