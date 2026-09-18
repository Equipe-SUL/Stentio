package com.example.stentio.core.controller;
import com.example.stentio.core.dto.IdiomaRequest;
import com.example.stentio.core.dto.IdiomaResponse;
import com.example.stentio.core.model.Idioma;
import com.example.stentio.core.service.IdiomaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(IdiomaController.BASE_PATH)
public class IdiomaController {
    static final String BASE_PATH = "/api/v1/idiomas";

    private final IdiomaService idiomaService;

    public IdiomaController(IdiomaService idiomaService){
        this.idiomaService = idiomaService;
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<IdiomaResponse> criar(@Valid @RequestBody IdiomaRequest request){
        IdiomaResponse response = idiomaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
