package com.stentio.core.service;

import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.dto.SolicitacaoPublicaResponse;
import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.exception.SolicitacaoPublicaValidacaoException;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.StatusSolicitacao;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SolicitacaoPublicaCriacaoServiceTest {

    @Test
    void validaCatalogosResolveClienteEReutilizaCriacaoInterna() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        ClientePublicoService clientesPublicos = mock(ClientePublicoService.class);
        SolicitacaoService solicitacoes = mock(SolicitacaoService.class);
        SolicitacaoPublicaService service =
                new SolicitacaoPublicaService(categorias, idiomas, clientesPublicos, solicitacoes);

        UUID clienteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        UUID solicitacaoId = UUID.randomUUID();
        SolicitacaoPublicaRequest requisicao = new SolicitacaoPublicaRequest(
                "Empresa Exemplo", "Representante", "cliente@exemplo.com", null,
                "52998224725", categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato");

        Idioma origem = new Idioma();
        origem.setAtivo(true);
        Idioma destino = new Idioma();
        destino.setAtivo(true);
        ClienteResponse cliente = new ClienteResponse(
                clienteId, "Empresa existente", "Representante existente", "existente@exemplo.com",
                null, "52998224725", null, null);
        List<MultipartFile> arquivos = List.of(
                new MockMultipartFile("arquivos", "contrato.pdf", "application/pdf", new byte[]{1}));
        when(categorias.existsById(categoriaId)).thenReturn(true);
        when(idiomas.findById(idiomaOrigemId)).thenReturn(Optional.of(origem));
        when(idiomas.findById(idiomaDestinoId)).thenReturn(Optional.of(destino));
        when(clientesPublicos.buscarOuCriar(requisicao)).thenReturn(cliente);
        when(solicitacoes.criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                arquivos))
                .thenReturn(new SolicitacaoResponse(
                        solicitacaoId, null, null, null, null, null,
                        StatusSolicitacao.PENDENTE_ORCAMENTO, null, List.of()));

        SolicitacaoPublicaResponse resposta = service.criar(requisicao, arquivos);

        assertEquals(solicitacaoId, resposta.id());
        assertEquals(StatusSolicitacao.PENDENTE_ORCAMENTO, resposta.status());
        var ordem = inOrder(categorias, idiomas, clientesPublicos, solicitacoes);
        ordem.verify(categorias).existsById(categoriaId);
        ordem.verify(idiomas).findById(idiomaOrigemId);
        ordem.verify(idiomas).findById(idiomaDestinoId);
        ordem.verify(clientesPublicos).buscarOuCriar(requisicao);
        ordem.verify(solicitacoes).criar(
                new SolicitacaoRequest(clienteId, categoriaId, idiomaOrigemId, idiomaDestinoId, "Traduzir contrato"),
                arquivos);
    }

    @Test
    void naoResolveClienteQuandoAlgumaReferenciaNaoExiste() {
        CategoriaProjetoRepository categorias = mock(CategoriaProjetoRepository.class);
        IdiomaRepository idiomas = mock(IdiomaRepository.class);
        ClientePublicoService clientesPublicos = mock(ClientePublicoService.class);
        SolicitacaoService solicitacoes = mock(SolicitacaoService.class);
        SolicitacaoPublicaService service =
                new SolicitacaoPublicaService(categorias, idiomas, clientesPublicos, solicitacoes);
        UUID categoriaId = UUID.randomUUID();
        UUID idiomaOrigemId = UUID.randomUUID();
        UUID idiomaDestinoId = UUID.randomUUID();
        SolicitacaoPublicaRequest requisicao = new SolicitacaoPublicaRequest(
                "Empresa Exemplo", "Representante", "cliente@exemplo.com", null,
                "52998224725", categoriaId, idiomaOrigemId, idiomaDestinoId, null);
        when(categorias.existsById(categoriaId)).thenReturn(false);
        when(idiomas.findById(idiomaOrigemId)).thenReturn(Optional.empty());
        when(idiomas.findById(idiomaDestinoId)).thenReturn(Optional.empty());

        assertThrows(SolicitacaoPublicaValidacaoException.class, () -> service.criar(requisicao, List.of()));

        verifyNoInteractions(clientesPublicos, solicitacoes);
    }
}
