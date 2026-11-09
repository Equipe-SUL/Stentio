package com.stentio.core.controller;

import com.stentio.core.dto.AlocacaoEtapaRequest;
import com.stentio.core.dto.AlocacaoEtapaResponse;
import com.stentio.core.service.AlocacaoEtapaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * US-012: alocação de recurso a uma etapa.
 *
 * <p>Separado do controller de etapas da US-011 de propósito: são duas US com donos e
 * prazos diferentes, e a rota é a mesma árvore mas outro recurso. O perfil já vem da regra
 * de /api/v1/ordens-servico/** no SecurityConfig — ADMIN e GESTOR_PROJETO.
 */
@RestController
@RequestMapping(AlocacaoEtapaController.BASE_PATH)
public class AlocacaoEtapaController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/etapas/{etapaId}/alocacao";

    private final AlocacaoEtapaService alocacaoEtapaService;

    public AlocacaoEtapaController(AlocacaoEtapaService alocacaoEtapaService) {
        this.alocacaoEtapaService = alocacaoEtapaService;
    }

    /**
     * CA.3 e CA.4. Responde 200 e não 201: alocar não cria recurso novo, muda o estado de
     * uma etapa que já existe — e realocar passa pelo mesmo caminho.
     */
    @PostMapping
    @Operation(summary = "Aloca um recurso à etapa da ordem de serviço e define a data limite de entrega")
    public ResponseEntity<AlocacaoEtapaResponse> alocar(
            @PathVariable("osId") UUID osId,
            @PathVariable("etapaId") UUID etapaId,
            @RequestBody @Valid AlocacaoEtapaRequest request) {
        return ResponseEntity.ok(alocacaoEtapaService.alocar(osId, etapaId, request));
    }
}
