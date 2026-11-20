package com.stentio.core.controller;

import com.stentio.core.dto.DisparoEtapaResponse;
import com.stentio.core.service.DisparoEtapaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * US-013 CA.1: o gestor abre a etapa para todos os elegíveis.
 *
 * <p>Mora sob /api/v1/ordens-servico, então herda a regra de ADMIN e GESTOR_PROJETO do
 * SecurityConfig — disparar é decisão de quem gerencia o projeto, não de quem executa.
 */
@RestController
@RequestMapping(DisparoEtapaController.BASE_PATH)
public class DisparoEtapaController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/etapas/{etapaId}/disparo";

    private final DisparoEtapaService disparoEtapaService;

    public DisparoEtapaController(DisparoEtapaService disparoEtapaService) {
        this.disparoEtapaService = disparoEtapaService;
    }

    @PostMapping
    @Operation(summary = "Dispara a etapa para todos os recursos elegíveis")
    public ResponseEntity<DisparoEtapaResponse> disparar(
            @PathVariable("osId") UUID osId,
            @PathVariable("etapaId") UUID etapaId) {
        return ResponseEntity.ok(disparoEtapaService.disparar(osId, etapaId));
    }
}
