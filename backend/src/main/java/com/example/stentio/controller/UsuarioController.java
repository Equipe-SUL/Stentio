package com.example.stentio.controller;

import com.example.stentio.dto.UsuarioRequestDTO;
import com.example.stentio.dto.UsuarioResponseDTO;
import com.example.stentio.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UsuarioRequestDTO dadosLogin) {
        String token = usuarioService.login(dadosLogin);
        return ResponseEntity.ok(token);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@RequestBody @Valid UsuarioRequestDTO dto) {
        UsuarioResponseDTO criado = usuarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editar(@PathVariable UUID id, @RequestBody @Valid UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.editar(id, dto));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UsuarioResponseDTO> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.ativarOuDesativar(id, true));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.ativarOuDesativar(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

