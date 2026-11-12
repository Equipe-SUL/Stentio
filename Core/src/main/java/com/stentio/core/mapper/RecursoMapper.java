package com.stentio.core.mapper;

import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.dto.RecursoDisponivelResponse.PrecoDoPar;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.dto.RecursoResponse.IdiomaResumo;
import com.stentio.core.dto.RecursoResponse.PrecoResponse;
import com.stentio.core.dto.RecursoResponse.TipoServicoResumo;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.RecursoPreco;
import com.stentio.core.model.TipoServico;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// Converte entidade em resposta. Não consulta o banco: quem resolve ids em entidades é o service.
@Component
public class RecursoMapper {

    // Ordem estável na resposta, para a tela não "pular" itens entre uma consulta e outra.
    private static final Comparator<PrecoResponse> ORDEM_PRECOS = Comparator
            .comparing((PrecoResponse preco) -> preco.idiomaOrigem().nome())
            .thenComparing(preco -> preco.idiomaDestino().nome())
            .thenComparing(PrecoResponse::unidade);

    public RecursoResponse paraResponse(Recurso recurso) {
        return new RecursoResponse(
                recurso.getId(),
                recurso.getNome(),
                recurso.getEmail(),
                recurso.getTelefone(),
                recurso.isAtivo(),
                paraTiposServico(recurso),
                paraPrecos(recurso)
        );
    }

    /**
     * US-012 CA.2. Os preços vêm filtrados pelo par consultado: na tela de alocação só
     * interessa quanto custa aquele par, e mandar a tabela inteira de cada profissional
     * faria a lista crescer sem necessidade.
     */
    public RecursoDisponivelResponse paraDisponivel(Recurso recurso, UUID idiomaOrigemId, UUID idiomaDestinoId) {
        List<PrecoDoPar> precos = recurso.getPrecos().stream()
                .filter(preco -> preco.getIdiomaOrigem().getId().equals(idiomaOrigemId)
                        && preco.getIdiomaDestino().getId().equals(idiomaDestinoId))
                .map(preco -> new PrecoDoPar(preco.getUnidade(), preco.getValor()))
                .sorted(Comparator.comparing(PrecoDoPar::unidade))
                .toList();

        return new RecursoDisponivelResponse(
                recurso.getId(),
                recurso.getNome(),
                recurso.getEmail(),
                precos);
    }

    private List<TipoServicoResumo> paraTiposServico(Recurso recurso) {
        return recurso.getTiposServico().stream()
                .map(this::paraTipoServico)
                .sorted(Comparator.comparing(TipoServicoResumo::nome))
                .toList();
    }

    private List<PrecoResponse> paraPrecos(Recurso recurso) {
        return recurso.getPrecos().stream()
                .map(this::paraPreco)
                .sorted(ORDEM_PRECOS)
                .toList();
    }

    private TipoServicoResumo paraTipoServico(TipoServico tipo) {
        return new TipoServicoResumo(tipo.getId(), tipo.getNome());
    }

    private PrecoResponse paraPreco(RecursoPreco preco) {
        return new PrecoResponse(
                preco.getId(),
                paraIdioma(preco.getIdiomaOrigem()),
                paraIdioma(preco.getIdiomaDestino()),
                preco.getUnidade(),
                preco.getValor()
        );
    }

    private IdiomaResumo paraIdioma(Idioma idioma) {
        return new IdiomaResumo(idioma.getId(), idioma.getNome(), idioma.getCodigoIso());
    }
}
