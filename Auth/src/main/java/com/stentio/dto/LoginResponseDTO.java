package com.stentio.dto;

import com.stentio.model.Role;

public record LoginResponseDTO (
        String token,
        String tipo,
        long expiresIn,
        String email,
        Role role
){ }
