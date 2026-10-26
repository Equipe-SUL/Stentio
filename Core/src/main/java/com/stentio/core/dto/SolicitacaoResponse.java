package com.stentio.core.dto;

import com.stentio.core.model.StatusSolicitacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Traz os nomes junto dos ids para a tela de listagem não precisar de uma requisição por vínculo.
public record SolicitacaoResponse(
        UUID id,
        ClienteResumo cliente,
        CategoriaProjetoResumo categoriaProjeto,
        IdiomaResumo idiomaOrigem,
        IdiomaResumo idiomaDestino,
        String descricao,
        StatusSolicitacao status,
        LocalDateTime dataRecebimento,
        List<ArquivoResumo> arquivos
) {

    public record ClienteResumo(UUID id, String nomeEmpresa) {}

    public record CategoriaProjetoResumo(UUID id, String nome) {}

    public record IdiomaResumo(UUID id, String nome, String codigoIso) {}

    // "url" aponta para o endpoint autenticado de download, nunca para o caminho em disco.
    public record ArquivoResumo(UUID id, String nome, String url, String tipoConteudo, long tamanhoBytes) {}
}
