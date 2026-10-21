package com.stentio.dto;

import java.util.UUID;

import com.stentio.model.Role;
import com.stentio.model.Usuario;

public record UsuarioResponseDTO(UUID id, String nome, String email, Role role, boolean ativo) {
    public UsuarioResponseDTO(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isAtivo()
        );
    }
}