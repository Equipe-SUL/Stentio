package com.stentio.core.mapper;

import com.stentio.core.dto.TabelaPrecoResponse;
import com.stentio.core.dto.TabelaPrecoResponse.IdiomaResumo;
import com.stentio.core.dto.TabelaPrecoResponse.TipoServicoResumo;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.TabelaPreco;
import com.stentio.core.model.TipoServico;
import org.springframework.stereotype.Component;

// Converte entidade em resposta. Não consulta o banco: quem resolve ids em entidades é o service.
@Component
public class TabelaPrecoMapper {

    public TabelaPrecoResponse paraResponse(TabelaPreco tabelaPreco) {
        return new TabelaPrecoResponse(
                tabelaPreco.getId(),
                paraTipoServico(tabelaPreco.getTipoServico()),
                paraIdioma(tabelaPreco.getIdiomaOrigem()),
                paraIdioma(tabelaPreco.getIdiomaDestino()),
                tabelaPreco.getUnidade(),
                tabelaPreco.getValor()
        );
    }

    private TipoServicoResumo paraTipoServico(TipoServico tipo) {
        return new TipoServicoResumo(tipo.getId(), tipo.getNome());
    }

    private IdiomaResumo paraIdioma(Idioma idioma) {
        return new IdiomaResumo(idioma.getId(), idioma.getNome(), idioma.getCodigoIso());
    }
}
