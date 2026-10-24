package com.stentio.core.controller;

import com.stentio.core.dto.*;
import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.OrcamentoPublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/orcamentos")
@SecurityRequirements
public class OrcamentoPublicoController {
    private final OrcamentoPublicoService service;
    private final ArquivoStorageService arquivoStorageService;

    public OrcamentoPublicoController(OrcamentoPublicoService service, ArquivoStorageService arquivoStorageService) {
        this.service = service;
        this.arquivoStorageService = arquivoStorageService;
    }

    @GetMapping
    @Operation(summary = "Consulta orçamento pelo link do cliente", security = {})
    public ResponseEntity<OrcamentoPublicoResponse> consultar(@RequestParam(required = false) String token) {
        return resposta(service.consultar(token));
    }

    @GetMapping("/arquivos/{arquivoId}")
    @Operation(summary = "Baixa arquivo anexado à solicitação do orçamento pelo token público", security = {})
    public ResponseEntity<Resource> baixarArquivo(@RequestParam(required = false) String token,
                                                 @PathVariable UUID arquivoId) {
        OrcamentoPublicoService.ArquivoPublicoParaDownload arquivo = service.baixarArquivoPublico(token, arquivoId);
        Resource recurso = arquivoStorageService.carregar(arquivo.caminho());

        ContentDisposition anexo = ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        MediaType contentType = arquivo.tipoConteudo() == null || arquivo.tipoConteudo().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(arquivo.tipoConteudo());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .contentType(contentType)
                .contentLength(arquivo.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .body(recurso);
    }

    @PostMapping("/aprovar")
    @Operation(summary = "Aprova orçamento e consome o link", security = {})
    public ResponseEntity<OrcamentoDecisaoResponse> aprovar(@Valid @RequestBody OrcamentoAprovacaoRequest request) {
        return resposta(service.aprovar(request.token()));
    }

    @PostMapping("/rejeitar")
    @Operation(summary = "Rejeita orçamento e registra o motivo", security = {})
    public ResponseEntity<OrcamentoDecisaoResponse> rejeitar(@Valid @RequestBody OrcamentoRejeicaoRequest request) {
        return resposta(service.rejeitar(request.token(), request.motivo()));
    }

    private <T> ResponseEntity<T> resposta(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").body(body);
    }
}
