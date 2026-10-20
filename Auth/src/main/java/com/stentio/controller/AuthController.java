package com.stentio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stentio.dto.UsuarioResponseDTO;
import com.stentio.service.UsuarioService;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/eu")
    public ResponseEntity<UsuarioResponseDTO> eu() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email));
    }
}
