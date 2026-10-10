package com.stentio.core.controller;

import com.stentio.core.dto.*;
import com.stentio.core.service.OrcamentoPublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/orcamentos")
@SecurityRequirements
public class OrcamentoPublicoController {
    private final OrcamentoPublicoService service;

    public OrcamentoPublicoController(OrcamentoPublicoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta orçamento pelo link do cliente", security = {})
    public ResponseEntity<OrcamentoPublicoResponse> consultar(@RequestParam(required = false) String token) {
        return resposta(service.consultar(token));
    }

    @PostMapping("/aprovar")
    @Operation(summary = "Aprova orçamento e consome o link", security = {})
    public ResponseEntity<OrcamentoDecisaoResponse> aprovar(@Valid @RequestBody OrcamentoAprovacaoRequest request) {
        return resposta(service.aprovar(request.token()));
    }

    @PostMapping("/rejeitar")
    @Operation(summary = "Rejeita orçamento e registra o motivo", security = {})
    public ResponseEntity<OrcamentoDecisaoResponse> rejeitar(@Valid @RequestBody OrcamentoRejeicaoRequest request) {
        return resposta(service.rejeitar(request.token(), request.motivo()));
    }

    private <T> ResponseEntity<T> resposta(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").body(body);
    }
}
