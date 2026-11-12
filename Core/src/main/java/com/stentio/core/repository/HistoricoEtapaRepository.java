package com.stentio.core.repository;

import com.stentio.core.model.HistoricoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoEtapaRepository extends JpaRepository<HistoricoEtapa, UUID> {

    // Mais recente primeiro: é a ordem em que alguém lê um histórico para entender o agora.
    List<HistoricoEtapa> findByEtapaIdOrderByRealizadoEmDesc(UUID etapaId);
}
