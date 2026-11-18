package com.stentio.core.controller;

import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.dto.SolicitacaoPublicaResponse;
import com.stentio.core.service.SolicitacaoPublicaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitacaoPublicaResponse> criar(
            @Valid @ModelAttribute SolicitacaoPublicaRequest requisicao,
            @RequestPart(name = "arquivos", required = false) List<MultipartFile> arquivos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(requisicao, arquivos));
    }
}
