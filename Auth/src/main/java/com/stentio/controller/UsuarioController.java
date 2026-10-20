package com.stentio.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.stentio.config.AuthCookieService;
import com.stentio.dto.LoginRequestDTO;
import com.stentio.dto.LoginResponseDTO;
import com.stentio.dto.UsuarioRequestDTO;
import com.stentio.dto.UsuarioResponseDTO;
import com.stentio.dto.UsuarioUpdateRequestDTO;
import com.stentio.service.UsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthCookieService authCookieService;

    public UsuarioController(UsuarioService usuarioService, AuthCookieService authCookieService) {
        this.usuarioService = usuarioService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dadosLogin) {
        LoginResponseDTO resposta = usuarioService.login(dadosLogin);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.criar(resposta.token()).toString())
                .body(resposta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.limpar().toString())
                .build();
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
    public ResponseEntity<UsuarioResponseDTO> editar(@PathVariable UUID id, @RequestBody @Valid UsuarioUpdateRequestDTO dto) {
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

