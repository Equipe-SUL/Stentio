package com.stentio.core.controller;

import com.stentio.core.dto.EntregaFinalResponse;
import com.stentio.core.service.EntregaFinalService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * US-015: a última etapa manual do fluxo, antes do cliente receber o trabalho.
 *
 * <p>Sob /api/v1/ordens-servico, então vale a regra de ADMIN e GESTOR_PROJETO do
 * SecurityConfig (CA.1) sem precisar repeti-la aqui.
 */
@RestController
@RequestMapping(EntregaFinalController.BASE_PATH)
public class EntregaFinalController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/entrega-final";

    private final EntregaFinalService entregaFinalService;

    public EntregaFinalController(EntregaFinalService entregaFinalService) {
        this.entregaFinalService = entregaFinalService;
    }

    @GetMapping
    @Operation(summary = "Revisão final: etapas entregues e o pacote que vai ao cliente")
    public EntregaFinalResponse revisar(@PathVariable("osId") UUID osId) {
        return entregaFinalService.revisar(osId);
    }
}
