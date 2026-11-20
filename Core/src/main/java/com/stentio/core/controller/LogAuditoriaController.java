package com.stentio.core.controller;

import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.dto.LogAuditoriaPaginaResponse;
import com.stentio.core.model.EntidadeAuditada;
import com.stentio.core.service.LogAuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * US-019: consulta do log de auditoria.
 *
 * <p>RN.1: só existe GET. Sem PUT/PATCH/DELETE mapeados, o Spring responde 405 a qualquer
 * tentativa de alterar ou apagar um registro.
 * <p>RN.2: restrito ao perfil ADMIN no {@code SecurityConfig} (403 para os demais).
 */
@RestController
@RequestMapping(LogAuditoriaController.BASE_PATH)
public class LogAuditoriaController {

    static final String BASE_PATH = "/api/v1/auditoria";

    private final LogAuditoriaService service;

    public LogAuditoriaController(LogAuditoriaService service) {
        this.service = service;
    }

    @GetMapping("/logs")
    @Operation(summary = "Lista o log de auditoria de mudanças de status, do mais recente ao mais antigo, com filtros combináveis")
    public ResponseEntity<LogAuditoriaPaginaResponse> listar(
            @RequestParam(required = false) EntidadeAuditada entidade,
            @RequestParam(name = "entidade_id", required = false) UUID entidadeId,
            @RequestParam(name = "usuario_id", required = false) UUID usuarioId,
            @RequestParam(name = "data_inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(name = "data_fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LogAuditoriaFiltro filtro = new LogAuditoriaFiltro(entidade, entidadeId, usuarioId, dataInicio, dataFim, busca);
        return ResponseEntity.ok(service.listar(filtro, page, size));
    }
}
