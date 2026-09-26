package com.stentio.core.repository;

import com.stentio.core.model.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID> {
}