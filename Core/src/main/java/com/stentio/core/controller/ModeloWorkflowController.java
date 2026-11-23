package com.stentio.core.controller;

import com.stentio.core.dto.AlteracaoStatusRequest;
import com.stentio.core.dto.ModeloWorkflowRequest;
import com.stentio.core.dto.ModeloWorkflowResponse;
import com.stentio.core.service.ModeloWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

// US-011: cadastro dos modelos de workflow aplicáveis às ordens de serviço.
@RestController
@RequestMapping(ModeloWorkflowController.BASE_PATH)
public class ModeloWorkflowController {

    static final String BASE_PATH = "/api/v1/modelos-workflow";

    private final ModeloWorkflowService modeloWorkflowService;

    public ModeloWorkflowController(ModeloWorkflowService modeloWorkflowService) {
        this.modeloWorkflowService = modeloWorkflowService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um modelo de workflow com sua sequência de etapas")
    public ResponseEntity<ModeloWorkflowResponse> criar(@RequestBody @Valid ModeloWorkflowRequest request) {
        ModeloWorkflowResponse criado = modeloWorkflowService.criar(request);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista os modelos de workflow, com filtro opcional por ativo")
    public ResponseEntity<List<ModeloWorkflowResponse>> listar(
            @RequestParam(required = false) Boolean ativo) {
        return ResponseEntity.ok(modeloWorkflowService.listar(ativo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um modelo de workflow e suas etapas")
    public ResponseEntity<ModeloWorkflowResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(modeloWorkflowService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Substitui os dados e a sequência de etapas de um modelo")
    public ResponseEntity<ModeloWorkflowResponse> substituir(@PathVariable UUID id,
                                                             @RequestBody @Valid ModeloWorkflowRequest request) {
        return ResponseEntity.ok(modeloWorkflowService.substituir(id, request));
    }

    /**
     * Desativar em vez de excluir, pelo mesmo motivo do cadastro de recursos: um modelo já
     * aplicado faz parte do histórico de ordens de serviço e apagá-lo deixaria rastro quebrado.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Ativa ou desativa um modelo de workflow")
    public ResponseEntity<ModeloWorkflowResponse> alterarAtivacao(@PathVariable UUID id,
                                                                  @RequestBody @Valid AlteracaoStatusRequest request) {
        return ResponseEntity.ok(modeloWorkflowService.alterarAtivacao(id, request.ativo()));
    }
}
