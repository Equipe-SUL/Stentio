package com.stentio.core.controller;

import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.service.RecursosElegiveisEtapaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * A lista da tela de alocação. Fica sob /ordens-servico, então herda os perfis da rota no
 * SecurityConfig (Admin e Gestor de Projeto) — os mesmos que podem alocar.
 */
@RestController
@RequestMapping(RecursosElegiveisEtapaController.BASE_PATH)
public class RecursosElegiveisEtapaController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/etapas/{etapaId}/recursos-elegiveis";

    private final RecursosElegiveisEtapaService service;

    public RecursosElegiveisEtapaController(RecursosElegiveisEtapaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os recursos que podem ser alocados à etapa: ativos, com certificação "
            + "aprovada para o par de idiomas da solicitação e o tipo de serviço da etapa")
    public List<RecursoDisponivelResponse> listar(@PathVariable("osId") UUID osId,
                                                   @PathVariable("etapaId") UUID etapaId) {
        return service.listar(osId, etapaId);
    }
}
