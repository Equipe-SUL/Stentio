package com.stentio.core.controller;

import com.stentio.core.dto.DashboardResumoResponse;
import com.stentio.core.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DashboardController.BASE_PATH)
@Tag(name = "Dashboard", description = "Resumo executivo e pendências operacionais")
public class DashboardController {

    static final String BASE_PATH = "/api/v1/dashboard";

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Retorna os indicadores consolidados do painel central de monitoramento")
    public ResponseEntity<DashboardResumoResponse> resumo() {
        return ResponseEntity.ok(dashboardService.resumo());
    }
}
