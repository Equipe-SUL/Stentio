package com.example.stentio.dto;

import com.example.stentio.model.Role;
import com.example.stentio.model.Usuario;

public record UsuarioResponseDTO(Long id, String nome, String email, Role role, boolean ativo) {
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