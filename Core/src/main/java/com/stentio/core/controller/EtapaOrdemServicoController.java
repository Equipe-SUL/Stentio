package com.stentio.core.controller;

import com.stentio.core.dto.EtapasOrdemServicoRequest;
import com.stentio.core.dto.EtapasOrdemServicoResponse;
import com.stentio.core.service.EtapaOrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// US-011: estruturação das etapas de uma ordem de serviço.
@RestController
@RequestMapping(EtapaOrdemServicoController.BASE_PATH)
public class EtapaOrdemServicoController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/etapas";

    private final EtapaOrdemServicoService etapaOrdemServicoService;

    public EtapaOrdemServicoController(EtapaOrdemServicoService etapaOrdemServicoService) {
        this.etapaOrdemServicoService = etapaOrdemServicoService;
    }

    /**
     * CA.3. Substitui a sequência inteira, não acrescenta: a tela envia a lista final depois
     * de o gestor adicionar, remover e reordenar. Por isso é 200 e não 201 — o recurso
     * "etapas da OS" já existe, o que muda é o conteúdo dele.
     */
    @PostMapping
    @Operation(summary = "Define manualmente a sequência de etapas da ordem de serviço, substituindo a anterior")
    public ResponseEntity<EtapasOrdemServicoResponse> definirManualmente(
            @PathVariable("osId") UUID osId,
            @RequestBody @Valid EtapasOrdemServicoRequest request) {
        return ResponseEntity.ok(etapaOrdemServicoService.definirManualmente(osId, request));
    }

    // CA.2
    @PostMapping("/aplicar-modelo/{modeloId}")
    @Operation(summary = "Clona as etapas de um modelo de workflow para a ordem de serviço")
    public ResponseEntity<EtapasOrdemServicoResponse> aplicarModelo(
            @PathVariable("osId") UUID osId,
            @PathVariable("modeloId") UUID modeloId) {
        return ResponseEntity.ok(etapaOrdemServicoService.aplicarModelo(osId, modeloId));
    }
}
