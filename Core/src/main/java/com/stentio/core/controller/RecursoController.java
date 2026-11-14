package com.stentio.core.controller;

import com.stentio.core.dto.AlteracaoStatusRequest;
import com.stentio.core.dto.RecursoCriadoResponse;
import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.dto.RecursoFiltro;
import com.stentio.core.dto.RecursoRequest;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.service.RecursoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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
import java.util.Set;
import java.util.UUID;

// Sem DELETE de propósito (RN.4): recursos são apenas desativados.
@RestController
@RequestMapping(RecursoController.BASE_PATH)
public class RecursoController {

    static final String BASE_PATH = "/api/v1/recursos";

    // Ordenar por campos fora desta lista geraria erro 500 na consulta.
    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("nome", "email", "ativo");

    private final RecursoService recursoService;

    public RecursoController(RecursoService recursoService) {
        this.recursoService = recursoService;
    }

    @PostMapping
    public ResponseEntity<RecursoCriadoResponse> criar(@RequestBody @Valid RecursoRequest request) {
        RecursoResponse criado = recursoService.criar(request);
        // URI relativa: continua correta quando a requisição passa pelo API Gateway.
        URI localizacao = URI.create(BASE_PATH + "/" + criado.id());
        return ResponseEntity.created(localizacao).body(RecursoCriadoResponse.de(criado));
    }

    @GetMapping
    public PagedModel<RecursoResponse> listar(
            RecursoFiltro filtro,
            @PageableDefault(size = 20, sort = "nome") Pageable paginacao) {
        validarOrdenacao(paginacao.getSort());
        return new PagedModel<>(recursoService.listar(filtro, paginacao));
    }

    /**
     * US-012 CA.2: quem pode executar uma etapa com este par de idiomas e tipo de serviço.
     *
     * <p>Mapeada antes de /{id} por clareza; o Spring já prefere o caminho literal ao
     * template, então "disponiveis" nunca é lido como um id.
     */
    @GetMapping("/disponiveis")
    public List<RecursoDisponivelResponse> listarDisponiveis(
            @RequestParam(name = "idioma_origem_id") UUID idiomaOrigemId,
            @RequestParam(name = "idioma_destino_id") UUID idiomaDestinoId,
            @RequestParam(name = "tipo_servico_id") UUID tipoServicoId) {
        return recursoService.listarDisponiveis(idiomaOrigemId, idiomaDestinoId, tipoServicoId);
    }

    @GetMapping("/{id}")
    public RecursoResponse buscarPorId(@PathVariable UUID id) {
        return recursoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public RecursoResponse editar(@PathVariable UUID id, @RequestBody @Valid RecursoRequest request) {
        return recursoService.editar(id, request);
    }

    @PatchMapping("/{id}")
    public RecursoResponse alterarStatus(@PathVariable UUID id, @RequestBody @Valid AlteracaoStatusRequest request) {
        return recursoService.alterarStatus(id, request.ativo());
    }

    private static void validarOrdenacao(Sort ordenacao) {
        ordenacao.forEach(ordem -> {
            if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
                throw new RecursoInvalidoException("Não é possível ordenar por '" + ordem.getProperty() + "'");
            }
        });
    }
}
