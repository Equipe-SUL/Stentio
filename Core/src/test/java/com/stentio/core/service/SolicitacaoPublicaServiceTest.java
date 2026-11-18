package com.stentio.core.service;

import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.exception.SolicitacaoPublicaValidacaoException;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Idioma;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitacaoPublicaServiceTest {

    private static SolicitacaoPublicaService service(
            CategoriaProjetoRepository categorias,
            IdiomaRepository idiomas) {
        return new SolicitacaoPublicaService(
                categorias, idiomas, mock(ClientePublicoService.class), mock(SolicitacaoService.class));
    }

    @Test
    void deveRetornarCategoriasDisponiveisEIdiomasAtivosEmFormatoEnxuto() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoPublicaService service = service(categorias, idiomas);

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

    @Test
    void deveRetornarErrosDeCampoParaCategoriaEIdiomasInexistentesOuInativos() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoPublicaService service = service(categorias, idiomas);

        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        Idioma idiomaInativo = new Idioma();
        idiomaInativo.setId(idiomaDestinoId);
        idiomaInativo.setAtivo(false);

        when(categorias.existsById(categoriaId)).thenReturn(false);
        when(idiomas.findById(idiomaOrigemId)).thenReturn(Optional.empty());
        when(idiomas.findById(idiomaDestinoId)).thenReturn(Optional.of(idiomaInativo));

        var erro = assertThrows(SolicitacaoPublicaValidacaoException.class, () ->
                service.validarReferencias(new SolicitacaoPublicaRequest(
                        "Empresa", "Representante", "cliente@exemplo.com", null, "52998224725",
                        categoriaId, idiomaOrigemId, idiomaDestinoId, null)));

        assertEquals(
                List.of("categoriaProjetoId", "idiomaOrigemId", "idiomaDestinoId"),
                erro.erros().stream().map(campo -> campo.campo()).toList());
    }

    @Test
    void aceitaCategoriaExistenteEIdiomasAtivos() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        SolicitacaoPublicaService service = service(categorias, idiomas);

        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        Idioma idiomaOrigem = new Idioma();
        idiomaOrigem.setAtivo(true);
        Idioma idiomaDestino = new Idioma();
        idiomaDestino.setAtivo(true);

        when(categorias.existsById(categoriaId)).thenReturn(true);
        when(idiomas.findById(idiomaOrigemId)).thenReturn(Optional.of(idiomaOrigem));
        when(idiomas.findById(idiomaDestinoId)).thenReturn(Optional.of(idiomaDestino));

        assertDoesNotThrow(() -> service.validarReferencias(new SolicitacaoPublicaRequest(
                "Empresa", "Representante", "cliente@exemplo.com", null, "52998224725",
                categoriaId, idiomaOrigemId, idiomaDestinoId, null)));
    }
}
