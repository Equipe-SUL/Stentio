package com.stentio.core.service;

import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SolicitacaoPublicaService {

    private final CategoriaProjetoRepository categorias;
    private final IdiomaRepository idiomas;

    public SolicitacaoPublicaService(CategoriaProjetoRepository categorias, IdiomaRepository idiomas) {
        this.categorias = categorias;
        this.idiomas = idiomas;
    }

    public SolicitacaoOpcoesResponse obterOpcoes() {
        var categoriasProjeto = categorias.findAll(Sort.by("nome")).stream()
                .map(categoria -> new SolicitacaoOpcoesResponse.CategoriaOpcao(
                        categoria.getId(), categoria.getNome()))
                .toList();
        var idiomasAtivos = idiomas.findByAtivoTrueOrderByNomeAsc().stream()
                .map(idioma -> new SolicitacaoOpcoesResponse.IdiomaOpcao(
                        idioma.getId(), idioma.getNome(), idioma.getCodigoIso()))
                .toList();

        return new SolicitacaoOpcoesResponse(categoriasProjeto, idiomasAtivos);
    }
}
