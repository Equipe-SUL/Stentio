package com.stentio.core.controller;

import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.service.SolicitacaoPublicaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SolicitacaoPublicaController.BASE_PATH)
@SecurityRequirements
public class SolicitacaoPublicaController {

    static final String BASE_PATH = "/api/v1/public/solicitacoes";

    private final SolicitacaoPublicaService service;

    public SolicitacaoPublicaController(SolicitacaoPublicaService service) {
        this.service = service;
    }

    @GetMapping("/opcoes")
    public SolicitacaoOpcoesResponse obterOpcoes() {
        return service.obterOpcoes();
    }
}
