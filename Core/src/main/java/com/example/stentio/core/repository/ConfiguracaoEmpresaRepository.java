package com.example.stentio.core.repository;

import com.example.stentio.core.model.ConfiguracaoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoEmpresaRepository
        extends JpaRepository<ConfiguracaoEmpresa, Long> {
}