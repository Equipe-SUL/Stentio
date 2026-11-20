package com.stentio.core.controller;

import com.stentio.core.dto.AceiteEtapaResponse;
import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.dto.EntregaEtapaResponse;
import com.stentio.core.dto.EtapaStatusResponse;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.service.AceiteEtapaService;
import com.stentio.core.service.ArquivoStorageService;
import com.stentio.core.service.EntregaEtapaService;
import com.stentio.core.service.PortalRecursoService;
import com.stentio.core.service.RecusaEtapaService;
import com.stentio.core.support.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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
    private final RecusaEtapaService recusaEtapaService;
    private final EntregaEtapaService entregaEtapaService;
    private final ArquivoStorageService arquivoStorageService;

    public PortalRecursoController(PortalRecursoService portalRecursoService,
                                   AceiteEtapaService aceiteEtapaService,
                                   RecusaEtapaService recusaEtapaService,
                                   EntregaEtapaService entregaEtapaService,
                                   ArquivoStorageService arquivoStorageService) {
        this.portalRecursoService = portalRecursoService;
        this.aceiteEtapaService = aceiteEtapaService;
        this.recusaEtapaService = recusaEtapaService;
        this.entregaEtapaService = entregaEtapaService;
        this.arquivoStorageService = arquivoStorageService;
    }

    /**
     * US-013 CA.3: aceitar a etapa. Sem corpo: tudo o que a operação precisa é o id na
     * rota e quem está falando, que vem do token.
     */
    @PostMapping("/etapas/{etapaId}/aceitar")
    @Operation(summary = "Aceita uma etapa disparada ou alocada diretamente")
    public AceiteEtapaResponse aceitar(@PathVariable UUID etapaId, Authentication autenticacao) {
        return aceiteEtapaService.aceitar(etapaId, UsuarioAutenticado.de(autenticacao));
    }

    /** US-013 CA.6: recusar a alocação direta. A etapa volta a pendente. */
    @PostMapping("/etapas/{etapaId}/recusar")
    @Operation(summary = "Recusa uma etapa alocada diretamente ao profissional autenticado")
    public EtapaStatusResponse recusar(@PathVariable UUID etapaId, Authentication autenticacao) {
        return recusaEtapaService.recusar(etapaId, UsuarioAutenticado.de(autenticacao));
    }

    /** US-013 CA.5: entregar o arquivo da etapa. Um arquivo por entrega. */
    @PostMapping(value = {
            "/etapas/{etapaId}/entregar",
            "/etapas/{etapaId}/entrega"
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Entrega o arquivo da etapa aceita pelo profissional autenticado")
    public EntregaEtapaResponse entregar(@PathVariable UUID etapaId,
                                         @RequestPart("arquivo") MultipartFile arquivo,
                                         Authentication autenticacao) {
        return entregaEtapaService.entregar(etapaId, arquivo, UsuarioAutenticado.de(autenticacao));
    }

    @GetMapping("/etapas/{etapaId}/entrega/arquivo")
    @Operation(summary = "Baixa a entrega do profissional autenticado")
    public ResponseEntity<Resource> baixarEntrega(@PathVariable UUID etapaId, Authentication autenticacao) {
        EntregaEtapaService.ArquivoEntregue arquivo =
                entregaEtapaService.localizarEntregaDoRecurso(etapaId, UsuarioAutenticado.de(autenticacao));
        Resource conteudo = arquivoStorageService.carregar(arquivo.caminho());
        ContentDisposition anexo = ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .body(conteudo);
    }

    @GetMapping("/etapas")
    @Operation(summary = "Lista as etapas abertas e alocadas ao profissional autenticado")
    public List<DemandaRecursoResponse> minhasDemandas(Authentication autenticacao) {
        return portalRecursoService.minhasDemandas(UsuarioAutenticado.de(autenticacao));
    }

    @GetMapping("/perfil")
    @Operation(summary = "Devolve o perfil de competências do profissional autenticado")
    public RecursoResponse meuPerfil(Authentication autenticacao) {
        return portalRecursoService.meuPerfil(UsuarioAutenticado.de(autenticacao));
    }
}
