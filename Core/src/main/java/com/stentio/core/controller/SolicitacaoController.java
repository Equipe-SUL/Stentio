package com.stentio.core.controller;

import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.exception.SolicitacaoInvalidaException;
import com.stentio.core.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(SolicitacaoController.BASE_PATH)
public class SolicitacaoController {

    static final String BASE_PATH = "/api/v1/solicitacoes";

    // Ordenar por campos fora desta lista geraria erro 500 na consulta.
    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("dataRecebimento", "status");

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    // CA.2: multipart/form-data. Os campos vão como campos de formulário e os anexos na parte "arquivos".
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitacaoResponse> criar(
            @Valid @ModelAttribute SolicitacaoRequest request,
            @RequestPart(name = "arquivos", required = false) List<MultipartFile> arquivos) {

        SolicitacaoResponse criada = solicitacaoService.criar(request, arquivos);
        // URI relativa: continua correta quando a requisição passa pelo API Gateway.
        URI localizacao = URI.create(BASE_PATH + "/" + criada.id());
        return ResponseEntity.created(localizacao).body(criada);
    }

    @GetMapping
    public PagedModel<SolicitacaoResponse> listar(
            @PageableDefault(size = 20, sort = "dataRecebimento", direction = Sort.Direction.DESC) Pageable paginacao) {
        validarOrdenacao(paginacao.getSort());
        return new PagedModel<>(solicitacaoService.listar(paginacao));
    }

    @GetMapping("/{id}")
    public SolicitacaoResponse buscarPorId(@PathVariable UUID id) {
        return solicitacaoService.buscarPorId(id);
    }

    // O campo "url" de cada anexo aponta para cá: download autenticado, nunca arquivo servido direto do disco.
    @GetMapping("/{id}/arquivos/{arquivoId}")
    public ResponseEntity<Resource> baixarArquivo(@PathVariable UUID id, @PathVariable UUID arquivoId) {
        SolicitacaoService.ArquivoParaDownload arquivo = solicitacaoService.baixarArquivo(id, arquivoId);

        ContentDisposition anexo = ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(tipoDe(arquivo.tipoConteudo()))
                .contentLength(arquivo.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .body(arquivo.recurso());
    }

    // O content-type vem do cliente e pode chegar malformado; nesse caso cai no genérico.
    private static MediaType tipoDe(String tipoConteudo) {
        if (tipoConteudo == null || tipoConteudo.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(tipoConteudo);
        } catch (RuntimeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static void validarOrdenacao(Sort ordenacao) {
        ordenacao.forEach(ordem -> {
            if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
                throw new SolicitacaoInvalidaException("Não é possível ordenar por '" + ordem.getProperty() + "'");
            }
        });
    }
}
