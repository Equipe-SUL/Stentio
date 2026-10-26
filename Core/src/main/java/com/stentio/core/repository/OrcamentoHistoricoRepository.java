package com.stentio.core.repository;

import com.stentio.core.model.OrcamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrcamentoHistoricoRepository extends JpaRepository<OrcamentoHistorico, UUID> {
}
