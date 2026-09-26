package com.stentio.emailservice.controller;

import com.stentio.emailservice.dto.EmpresaPatchRequest;
import com.stentio.emailservice.dto.EmpresaRequest;
import com.stentio.emailservice.dto.EmpresaResponse;
import com.stentio.emailservice.exception.LogoNaoEncontradaException;
import com.stentio.emailservice.service.ConfiguracaoEmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/empresa")
public class ConfiguracaoEmpresaController {

    private final ConfiguracaoEmpresaService service;

    public ConfiguracaoEmpresaController(ConfiguracaoEmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<EmpresaResponse> buscar() {
        return ResponseEntity.ok(service.buscar());
    }

    @PutMapping
    public ResponseEntity<EmpresaResponse> salvar(
            @Valid @RequestBody EmpresaRequest request
    ) {
        return ResponseEntity.ok(service.salvar(request));
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmpresaResponse> salvarLogo(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(service.salvarLogo(arquivo));
    }

    // O front aponta o <Image> para esta rota, então sem ela a prévia nunca carregava.
    @GetMapping("/logo")
    public ResponseEntity<byte[]> buscarLogo() {

        byte[] logo = service.obterLogo();

        if (logo == null || logo.length == 0) {
            throw new LogoNaoEncontradaException();
        }

        // no-store, e não cache de longa duração: a logo muda no PUT /logo. Com
        // Cache-Control público de 1 hora, um reload da página ou um cache de
        // imagem do lado do cliente devolvia os bytes antigos — o usuário via a
        // mensagem "Logo atualizada com sucesso" e continuava vendo a anterior.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(service.obterContentTypeLogo()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(logo);
    }

    @PatchMapping
    public ResponseEntity<EmpresaResponse> atualizarParcialmente(
            @Valid @RequestBody EmpresaPatchRequest request
    ) {
        return ResponseEntity.ok(service.atualizarParcialmente(request));
    }
}
