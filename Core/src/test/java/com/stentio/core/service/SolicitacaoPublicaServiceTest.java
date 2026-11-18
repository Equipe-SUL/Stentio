package com.stentio.core.service;

import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Idioma;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitacaoPublicaServiceTest {

    @Test
    void deveRetornarCategoriasDisponiveisEIdiomasAtivosEmFormatoEnxuto() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoPublicaService service = new SolicitacaoPublicaService(categorias, idiomas);

        UUID categoriaId = UUID.randomUUID();
        CategoriaProjeto categoria = new CategoriaProjeto();
        categoria.setId(categoriaId);
        categoria.setNome("Contrato");

        UUID idiomaId = UUID.randomUUID();
        Idioma idioma = new Idioma();
        idioma.setId(idiomaId);
        idioma.setNome("Português");
        idioma.setCodigoIso("pt");
        idioma.setAtivo(true);

        when(categorias.findAll(Sort.by("nome"))).thenReturn(List.of(categoria));
        when(idiomas.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(idioma));

        SolicitacaoOpcoesResponse resposta = service.obterOpcoes();

        assertEquals(
                List.of(new SolicitacaoOpcoesResponse.CategoriaOpcao(categoriaId, "Contrato")),
                resposta.categoriasProjeto());
        assertEquals(
                List.of(new SolicitacaoOpcoesResponse.IdiomaOpcao(idiomaId, "Português", "pt")),
                resposta.idiomas());
        verify(categorias).findAll(Sort.by("nome"));
        verify(idiomas).findByAtivoTrueOrderByNomeAsc();
    }
}
