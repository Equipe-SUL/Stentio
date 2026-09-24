package com.example.stentio.core.repository;

import com.example.stentio.core.model.TabelaPreco;
import com.example.stentio.core.model.UnidadeCobranca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TabelaPrecoRepository extends JpaRepository<TabelaPreco, UUID> {

    // RN.1 na criação
    boolean existsByTipoServicoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndUnidade(
            UUID tipoServicoId, UUID idiomaOrigemId, UUID idiomaDestinoId, UnidadeCobranca unidade);

    // RN.1 na edição: ignora a própria entrada que está sendo editada
    boolean existsByTipoServicoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndUnidadeAndIdNot(
            UUID tipoServicoId, UUID idiomaOrigemId, UUID idiomaDestinoId, UnidadeCobranca unidade, UUID id);

    // Traz tipo de serviço e idiomas na mesma consulta da listagem
    @Override
    @EntityGraph(attributePaths = {"tipoServico", "idiomaOrigem", "idiomaDestino"})
    Page<TabelaPreco> findAll(Pageable pageable);
}
