package com.stentio.core.controller;

import com.stentio.core.dto.OrcamentoAprovacaoResponse;
import com.stentio.core.service.OrcamentoEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orcamentos")
public class OrcamentoEnvioController {
    private final OrcamentoEnvioService service;

    public OrcamentoEnvioController(OrcamentoEnvioService service) {
        this.service = service;
    }

    @PostMapping("/{id}/enviar-aprovacao")
    @Operation(summary = "Envia orçamento para aprovação por e-mail")
    public ResponseEntity<OrcamentoAprovacaoResponse> enviarParaAprovacao(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.enviarParaAprovacao(id));
    }
}
