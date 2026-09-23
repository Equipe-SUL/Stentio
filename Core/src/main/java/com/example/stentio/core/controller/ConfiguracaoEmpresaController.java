package com.example.stentio.core.controller;

import com.example.stentio.core.dto.EmpresaRequest;
import com.example.stentio.core.dto.EmpresaResponse;
import com.example.stentio.core.service.ConfiguracaoEmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/empresa")
public class ConfiguracaoEmpresaController {

    private final ConfiguracaoEmpresaService service;

    public ConfiguracaoEmpresaController(
            ConfiguracaoEmpresaService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<EmpresaResponse> buscar() {
        return ResponseEntity.ok(service.buscar());
    }

    @PutMapping
    public ResponseEntity<EmpresaResponse> salvar(
            @Valid @RequestBody EmpresaRequest request
    ) {
        return ResponseEntity.ok(service.salvar(request));
    }

    @PutMapping(
            value = "/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<EmpresaResponse> salvarLogo(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(service.salvarLogo(arquivo));
    }
}