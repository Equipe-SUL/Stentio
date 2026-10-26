package com.stentio.core.mapper;

import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.dto.SolicitacaoResponse.ArquivoResumo;
import com.stentio.core.dto.SolicitacaoResponse.CategoriaProjetoResumo;
import com.stentio.core.dto.SolicitacaoResponse.ClienteResumo;
import com.stentio.core.dto.SolicitacaoResponse.IdiomaResumo;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Solicitacao;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// Converte entidade em resposta. Não consulta o banco: quem resolve ids em entidades é o service.
@Component
public class SolicitacaoMapper {

    public SolicitacaoResponse paraResponse(Solicitacao solicitacao) {
        return new SolicitacaoResponse(
                solicitacao.getId(),
                paraCliente(solicitacao.getCliente()),
                paraCategoria(solicitacao.getCategoriaProjeto()),
                paraIdioma(solicitacao.getIdiomaOrigem()),
                paraIdioma(solicitacao.getIdiomaDestino()),
                solicitacao.getDescricao(),
                solicitacao.getStatus(),
                solicitacao.getDataRecebimento(),
                paraArquivos(solicitacao)
        );
    }

    private List<ArquivoResumo> paraArquivos(Solicitacao solicitacao) {
        return solicitacao.getArquivos().stream()
                // Ordem estável: a lista não "pula" entre uma consulta e outra.
                .sorted(Comparator.comparing(ArquivoSolicitacao::getEnviadoEm)
                        .thenComparing(ArquivoSolicitacao::getNomeOriginal))
                .map(arquivo -> paraArquivo(solicitacao, arquivo))
                .toList();
    }

    private ArquivoResumo paraArquivo(Solicitacao solicitacao, ArquivoSolicitacao arquivo) {
        String url = "/api/v1/solicitacoes/" + solicitacao.getId() + "/arquivos/" + arquivo.getId();
        return new ArquivoResumo(
                arquivo.getId(),
                arquivo.getNomeOriginal(),
                url,
                arquivo.getTipoConteudo(),
                arquivo.getTamanhoBytes()
        );
    }

    private ClienteResumo paraCliente(Cliente cliente) {
        return new ClienteResumo(cliente.getId(), cliente.getNomeEmpresa());
    }

    private CategoriaProjetoResumo paraCategoria(CategoriaProjeto categoria) {
        return new CategoriaProjetoResumo(categoria.getId(), categoria.getNome());
    }

    private IdiomaResumo paraIdioma(Idioma idioma) {
        return new IdiomaResumo(idioma.getId(), idioma.getNome(), idioma.getCodigoIso());
    }
}
