package com.example.stentio.core.repository;

import com.example.stentio.core.model.Idioma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdiomaRepository extends JpaRepository<Idioma, UUID> {
}
