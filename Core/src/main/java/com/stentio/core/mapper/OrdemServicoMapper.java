package com.stentio.core.mapper;

import com.stentio.core.dto.OrdemServicoDetalheResponse;
import com.stentio.core.dto.OrdemServicoDetalheResponse.Arquivo;
import com.stentio.core.dto.OrdemServicoResumoResponse;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.support.CodigoVisual;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// Converte entidade em resposta. Não consulta o banco: quem carrega as entidades é o service.
@Component
public class OrdemServicoMapper {

    public OrdemServicoResumoResponse paraResumo(OrdemServico ordem) {
        // A OS não guarda o cliente: ele vem do orçamento que a originou.
        Cliente cliente = ordem.getOrcamento().getCliente();
        return new OrdemServicoResumoResponse(
                ordem.getId(),
                codigoVisual(ordem.getCriadoEm(), ordem.getId()),
                ordem.getStatus(),
                cliente.getId(),
                cliente.getNomeEmpresa(),
                ordem.getCriadoEm()
        );
    }

    public OrdemServicoDetalheResponse paraDetalhe(OrdemServico ordem) {
        Cliente cliente = ordem.getOrcamento().getCliente();

        return new OrdemServicoDetalheResponse(
                ordem.getId(),
                codigoVisual(ordem.getCriadoEm(), ordem.getId()),
                ordem.getStatus(),
                ordem.getOrcamento().getId(),
                ordem.getCriadoEm(),
                new OrdemServicoDetalheResponse.Cliente(cliente.getId(), cliente.getNomeEmpresa()),
                paraSolicitacao(ordem.getSolicitacao())
        );
    }

    private OrdemServicoDetalheResponse.Solicitacao paraSolicitacao(Solicitacao solicitacao) {
        return new OrdemServicoDetalheResponse.Solicitacao(
                solicitacao.getId(),
                solicitacao.getDescricao(),
                solicitacao.getCategoriaProjeto().getNome(),
                solicitacao.getIdiomaOrigem().getNome(),
                solicitacao.getIdiomaDestino().getNome(),
                paraArquivos(solicitacao)
        );
    }

    private List<Arquivo> paraArquivos(Solicitacao solicitacao) {
        return solicitacao.getArquivos().stream()
                // Mesma ordem estável da US-09, para a lista não "pular" entre consultas.
                .sorted(Comparator.comparing(ArquivoSolicitacao::getEnviadoEm)
                        .thenComparing(ArquivoSolicitacao::getNomeOriginal))
                .map(arquivo -> paraArquivo(solicitacao, arquivo))
                .toList();
    }

    private Arquivo paraArquivo(Solicitacao solicitacao, ArquivoSolicitacao arquivo) {
        // Reaproveita o download autenticado da US-09 em vez de expor o caminho em disco.
        String url = "/api/v1/solicitacoes/" + solicitacao.getId() + "/arquivos/" + arquivo.getId();
        return new Arquivo(arquivo.getId(), arquivo.getNomeOriginal(), url);
    }

    // Código legível de exibição (ex.: OS-2026-A1B2C3). A regra virou CodigoVisual porque o
    // orçamento passou a precisar do mesmo formato no e-mail da US-13.
    static String codigoVisual(Instant criadoEm, UUID id) {
        return CodigoVisual.gerar("OS", criadoEm, id);
    }
}
