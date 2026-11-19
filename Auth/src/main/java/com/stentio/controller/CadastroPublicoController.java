package com.stentio.controller;

import com.stentio.dto.AutoCadastroRequestDTO;
import com.stentio.dto.UsuarioResponseDTO;
import com.stentio.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-023: a única rota do Auth que responde sem autenticação além do login.
 *
 * <p>Fica em um controller separado de propósito. O {@code UsuarioController} inteiro está
 * sob {@code hasRole("ADMIN")} pelo caminho {@code /api/v1/usuarios/**}; acrescentar um
 * método público ali exigiria abrir uma exceção dentro de um bloco que hoje se lê de
 * relance como "só admin". Caminho separado, regra separada, risco visível.
 */
@RestController
@RequestMapping("/api/v1/public/usuarios")
@SecurityRequirements
public class CadastroPublicoController {

    private final UsuarioService usuarioService;

    public CadastroPublicoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Cria a conta de acesso de um profissional, com papel RECURSO")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@RequestBody @Valid AutoCadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.autoCadastrarRecurso(dto));
    }
}
