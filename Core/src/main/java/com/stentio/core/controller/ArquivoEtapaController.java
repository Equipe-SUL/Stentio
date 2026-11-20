package com.stentio.core.controller;

import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.EntregaEtapaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * US-013 CA.5: a equipe baixa o arquivo entregue.
 *
 * <p>Sob /api/v1/ordens-servico, então vale a regra de ADMIN e GESTOR_PROJETO do
 * SecurityConfig.
 */
@RestController
@RequestMapping(ArquivoEtapaController.BASE_PATH)
public class ArquivoEtapaController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/etapas/{etapaId}/arquivo";

    private final EntregaEtapaService entregaEtapaService;
    private final ArquivoStorageService arquivoStorageService;

    public ArquivoEtapaController(EntregaEtapaService entregaEtapaService,
                                  ArquivoStorageService arquivoStorageService) {
        this.entregaEtapaService = entregaEtapaService;
        this.arquivoStorageService = arquivoStorageService;
    }

    @GetMapping
    @Operation(summary = "Baixa o arquivo entregue na etapa")
    public ResponseEntity<Resource> baixar(@PathVariable("osId") UUID osId,
                                           @PathVariable("etapaId") UUID etapaId) {
        EntregaEtapaService.ArquivoEntregue arquivo = entregaEtapaService.localizarEntrega(osId, etapaId);
        Resource conteudo = arquivoStorageService.carregar(arquivo.caminho());

        ContentDisposition anexo = ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        // Sempre octet-stream e sempre attachment: o arquivo veio de fora, e devolvê-lo
        // com o tipo que o remetente declarou é o que transforma um upload em página
        // executada no domínio da aplicação.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .body(conteudo);
    }
}
