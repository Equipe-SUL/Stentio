package com.stentio.core.controller;

import com.stentio.core.dto.AceiteEtapaResponse;
import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.service.AceiteEtapaService;
import com.stentio.core.service.PortalRecursoService;
import com.stentio.core.support.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * US-023 CA.5: o portal do profissional.
 *
 * <p>Caminho no singular, {@code /api/v1/recurso}, separado do cadastro em
 * {@code /api/v1/recursos}: lá a equipe administra todos os profissionais, aqui a pessoa
 * vê a si mesma. Caminhos distintos deixam a regra de acesso legível no SecurityConfig,
 * em vez de depender de exceção dentro de um prefixo que já é de staff.
 *
 * <p>Nenhum método recebe id. Quem está falando vem do token.
 */
@RestController
@RequestMapping(PortalRecursoController.BASE_PATH)
public class PortalRecursoController {

    static final String BASE_PATH = "/api/v1/recurso";

    private final PortalRecursoService portalRecursoService;
    private final AceiteEtapaService aceiteEtapaService;

    public PortalRecursoController(PortalRecursoService portalRecursoService,
                                   AceiteEtapaService aceiteEtapaService) {
        this.portalRecursoService = portalRecursoService;
        this.aceiteEtapaService = aceiteEtapaService;
    }

    /**
     * US-013 CA.3: aceitar a etapa. Sem corpo: tudo o que a operação precisa é o id na
     * rota e quem está falando, que vem do token.
     */
    @PostMapping("/etapas/{etapaId}/aceitar")
    @Operation(summary = "Aceita uma etapa disparada; quem aceitar primeiro fica com ela")
    public AceiteEtapaResponse aceitar(@PathVariable UUID etapaId, Authentication autenticacao) {
        return aceiteEtapaService.aceitar(etapaId, UsuarioAutenticado.de(autenticacao));
    }

    @GetMapping("/etapas")
    @Operation(summary = "Lista as etapas alocadas ao profissional autenticado")
    public List<DemandaRecursoResponse> minhasDemandas(Authentication autenticacao) {
        return portalRecursoService.minhasDemandas(UsuarioAutenticado.de(autenticacao));
    }

    @GetMapping("/perfil")
    @Operation(summary = "Devolve o perfil de competências do profissional autenticado")
    public RecursoResponse meuPerfil(Authentication autenticacao) {
        return portalRecursoService.meuPerfil(UsuarioAutenticado.de(autenticacao));
    }
}
