package com.stentio.core.service;

import com.stentio.core.dto.ClienteResponse;
import com.stentio.core.dto.SolicitacaoOpcoesResponse;
import com.stentio.core.dto.SolicitacaoPublicaRequest;
import com.stentio.core.dto.SolicitacaoPublicaResponse;
import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.exception.ErroResponse;
import com.stentio.core.exception.SolicitacaoPublicaValidacaoException;
import com.stentio.core.model.Idioma;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.IdiomaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SolicitacaoPublicaService {

    private final CategoriaProjetoRepository categorias;
    private final IdiomaRepository idiomas;
    private final ClientePublicoService clientesPublicos;
    private final SolicitacaoService solicitacoes;

    public SolicitacaoPublicaService(
            CategoriaProjetoRepository categorias,
            IdiomaRepository idiomas,
            ClientePublicoService clientesPublicos,
            SolicitacaoService solicitacoes) {
        this.categorias = categorias;
        this.idiomas = idiomas;
        this.clientesPublicos = clientesPublicos;
        this.solicitacoes = solicitacoes;
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

    @Transactional
    public SolicitacaoPublicaResponse criar(SolicitacaoPublicaRequest requisicao, List<MultipartFile> arquivos) {
        validarReferencias(requisicao);

        ClienteResponse cliente = clientesPublicos.buscarOuCriar(requisicao);
        var solicitacao = solicitacoes.criar(
                new SolicitacaoRequest(
                        cliente.id(),
                        requisicao.categoriaProjetoId(),
                        requisicao.idiomaOrigemId(),
                        requisicao.idiomaDestinoId(),
                        requisicao.descricao()),
                arquivos);

        return new SolicitacaoPublicaResponse(solicitacao.id(), solicitacao.status());
    }

    public void validarReferencias(SolicitacaoPublicaRequest requisicao) {
        List<ErroResponse.CampoErro> erros = new ArrayList<>();

        if (requisicao.categoriaProjetoId() == null) {
            adicionarErro(erros, "categoriaProjetoId", "Categoria do projeto é obrigatória");
        } else if (!categorias.existsById(requisicao.categoriaProjetoId())) {
            adicionarErro(erros, "categoriaProjetoId", "Categoria do projeto não encontrada");
        }

        validarIdioma(requisicao.idiomaOrigemId(), "idiomaOrigemId", "Idioma de origem", erros);
        validarIdioma(requisicao.idiomaDestinoId(), "idiomaDestinoId", "Idioma de destino", erros);

        if (Objects.equals(requisicao.idiomaOrigemId(), requisicao.idiomaDestinoId())
                && requisicao.idiomaOrigemId() != null) {
            adicionarErro(erros, "idiomaDestinoId", "Os idiomas de origem e destino devem ser diferentes");
        }

        if (!erros.isEmpty()) {
            throw new SolicitacaoPublicaValidacaoException(erros);
        }
    }

    private void validarIdioma(
            UUID idiomaId,
            String campo,
            String rotulo,
            List<ErroResponse.CampoErro> erros) {
        if (idiomaId == null) {
            adicionarErro(erros, campo, rotulo + " é obrigatório");
            return;
        }

        Idioma idioma = idiomas.findById(idiomaId).orElse(null);
        if (idioma == null) {
            adicionarErro(erros, campo, rotulo + " não encontrado");
        } else if (!Boolean.TRUE.equals(idioma.getAtivo())) {
            adicionarErro(erros, campo, rotulo + " está inativo");
        }
    }

    private static void adicionarErro(List<ErroResponse.CampoErro> erros, String campo, String mensagem) {
        erros.add(new ErroResponse.CampoErro(campo, mensagem));
    }
}
