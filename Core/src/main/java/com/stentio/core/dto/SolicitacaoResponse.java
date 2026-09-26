package com.stentio.core.dto;

import com.stentio.core.model.Solicitacao;
import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoResponse(
        UUID id,
        UUID tipoServicoId,
        UUID idiomaOrigemId,
        UUID idiomaDestinoId,
        String descricao,
        String status,
        LocalDateTime dataRecebimento
) {
    public SolicitacaoResponse(Solicitacao solicitacao){
        this(
                solicitacao.getId(),
                solicitacao.getTipoServico().getId(),
                solicitacao.getIdiomaOrigem().getId(),
                solicitacao.getIdiomaDestino().getId(),
                solicitacao.getDescricao(),
                solicitacao.getStatus().name(),
                solicitacao.getDataRecebimento()
        );
    }
}