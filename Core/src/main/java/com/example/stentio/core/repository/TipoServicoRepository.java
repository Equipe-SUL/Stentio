package com.example.stentio.core.repository;

import com.example.stentio.core.model.TipoServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoServicoRepository extends JpaRepository<TipoServico, UUID> {
}
