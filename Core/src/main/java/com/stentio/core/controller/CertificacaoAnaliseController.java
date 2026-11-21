package com.stentio.core.controller;

import com.stentio.core.dto.CertificacaoPaginaResponse;
import com.stentio.core.dto.CertificacaoResponse;
import com.stentio.core.dto.RejeitarCertificacaoRequest;
import com.stentio.core.model.StatusCertificacao;
import com.stentio.core.service.CertificacaoAnaliseService;
import com.stentio.core.support.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * US-024 CA.2 e CA.6: a análise das certificações, restrita a quem decide alocação.
 *
 * <p>O caminho fica sob /api/v1/recursos, mas o SecurityConfig tem um matcher próprio
 * para ele, declarado antes da regra geral: lá, qualquer perfil da equipe faz GET, e
 * aqui ATENDENTE e FINANCEIRO não podem nem ver a fila nem baixar documentos.
 */
@RestController
@RequestMapping(CertificacaoAnaliseController.BASE_PATH)
public class CertificacaoAnaliseController {

    static final String BASE_PATH = "/api/v1/recursos/certificacoes";

    private final CertificacaoAnaliseService certificacaoAnaliseService;

    public CertificacaoAnaliseController(CertificacaoAnaliseService certificacaoAnaliseService) {
        this.certificacaoAnaliseService = certificacaoAnaliseService;
    }

    @GetMapping
    @Operation(summary = "Fila de certificações para análise, mais antigas primeiro")
    public CertificacaoPaginaResponse listar(
            @RequestParam(name = "status", required = false) StatusCertificacao status,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina,
            @RequestParam(name = "tamanho_pagina", defaultValue = "20") int tamanhoPagina) {
        return certificacaoAnaliseService.listarParaAnalise(status, pagina, tamanhoPagina);
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprova a certificação e libera o par para alocação e disparo")
    public CertificacaoResponse aprovar(@PathVariable("id") UUID id, Authentication autenticacao) {
        return certificacaoAnaliseService.aprovar(id, UsuarioAutenticado.de(autenticacao));
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeita a certificação com motivo e avisa o profissional")
    public CertificacaoResponse rejeitar(@PathVariable("id") UUID id,
                                         @Valid @RequestBody RejeitarCertificacaoRequest corpo,
                                         Authentication autenticacao) {
        return certificacaoAnaliseService.rejeitar(id, corpo.motivo(), UsuarioAutenticado.de(autenticacao));
    }
}
