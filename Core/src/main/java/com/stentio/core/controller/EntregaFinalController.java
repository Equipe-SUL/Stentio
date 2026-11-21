package com.stentio.core.controller;

import com.stentio.core.dto.AprovacaoEntregaFinalResponse;
import com.stentio.core.dto.AprovarEntregaFinalRequest;
import com.stentio.core.dto.EntregaFinalResponse;
import com.stentio.core.service.AprovacaoEntregaFinalService;
import com.stentio.core.service.EntregaFinalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.stentio.core.support.UsuarioAutenticado;

import java.util.UUID;

/**
 * US-015: a última etapa manual do fluxo, antes do cliente receber o trabalho.
 *
 * <p>Sob /api/v1/ordens-servico, então vale a regra de ADMIN e GESTOR_PROJETO do
 * SecurityConfig (CA.1) sem precisar repeti-la aqui.
 */
@RestController
@RequestMapping(EntregaFinalController.BASE_PATH)
public class EntregaFinalController {

    static final String BASE_PATH = "/api/v1/ordens-servico/{osId}/entrega-final";

    private final EntregaFinalService entregaFinalService;
    private final AprovacaoEntregaFinalService aprovacaoEntregaFinalService;

    public EntregaFinalController(EntregaFinalService entregaFinalService,
                                  AprovacaoEntregaFinalService aprovacaoEntregaFinalService) {
        this.entregaFinalService = entregaFinalService;
        this.aprovacaoEntregaFinalService = aprovacaoEntregaFinalService;
    }

    @GetMapping
    @Operation(summary = "Revisão final: etapas entregues e o pacote que vai ao cliente")
    public EntregaFinalResponse revisar(@PathVariable("osId") UUID osId) {
        return entregaFinalService.revisar(osId);
    }

    /**
     * US-015 CA.7: aceita a aprovação; o encerramento vem depois, com o ACK do broker.
     *
     * <p>202 e não 200: quando esta resposta sai, a OS ainda está aguardando verificação.
     */
    @PostMapping("/aprovar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Aprova a entrega final e enfileira o envio ao cliente")
    public AprovacaoEntregaFinalResponse aprovar(@PathVariable("osId") UUID osId,
                                                 @Valid @RequestBody(required = false)
                                                 AprovarEntregaFinalRequest corpo,
                                                 Authentication autenticacao) {
        return aprovacaoEntregaFinalService.aprovar(
                osId,
                corpo == null ? null : corpo.mensagem(),
                UsuarioAutenticado.de(autenticacao));
    }
}
