package com.stentio.core.controller;
import com.stentio.core.dto.AlteracaoStatusRequest;
import com.stentio.core.dto.IdiomaRequest;
import com.stentio.core.dto.IdiomaResponse;
import com.stentio.core.model.Idioma;
import com.stentio.core.service.IdiomaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(IdiomaController.BASE_PATH)
public class IdiomaController {
    static final String BASE_PATH = "/api/v1/idiomas";

    private final IdiomaService idiomaService;

    public IdiomaController(IdiomaService idiomaService){
        this.idiomaService = idiomaService;
    }

    @PostMapping
    public ResponseEntity<IdiomaResponse> criar(@Valid @RequestBody IdiomaRequest request){
        IdiomaResponse response = idiomaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @GetMapping
    public ResponseEntity<Page<IdiomaResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            Pageable pageable) {
        // passa os parâmetros para a camada de regra de negócio
        Page<IdiomaResponse> pagina = idiomaService.listar(nome, ativo, pageable);

        // devolve o resultado com status HTTP 200 (OK)
        return ResponseEntity.ok(pagina);

    }

    @PutMapping("/{id}")
    public ResponseEntity<IdiomaResponse> alterar(@PathVariable UUID id, @Valid @RequestBody IdiomaRequest request){
        idiomaService.editar(id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IdiomaResponse> alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlteracaoStatusRequest request) {
        IdiomaResponse resposta = idiomaService.alterarStatus(id, request.ativo());
        return ResponseEntity.ok(resposta);


    }

}
