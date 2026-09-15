package com.example.stentio.dto;

import com.example.stentio.model.Role;

public record LoginResponseDTO (
        String token,
        String tipo,
        long expiresIn,
        String email,
        Role role
){ }
