package com.stentio.core.service;

import com.stentio.core.dto.SolicitacaoRequest;
import com.stentio.core.dto.SolicitacaoResponse;
import com.stentio.core.exception.ReferenciaInvalidaException;
import com.stentio.core.exception.SolicitacaoNaoEncontradaException;
import com.stentio.core.mapper.SolicitacaoMapper;
import com.stentio.core.model.ArquivoArmazenado;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.model.Cliente;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.repository.CategoriaProjetoRepository;
import com.stentio.core.repository.ClienteRepository;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.SolicitacaoRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// Leitura por padrão; os métodos que alteram dados sobrescrevem com @Transactional de escrita.
@Service
@Transactional(readOnly = true)
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaProjetoRepository categoriaProjetoRepository;
    private final IdiomaRepository idiomaRepository;
    private final ArquivoStorageService arquivoStorageService;
    private final SolicitacaoMapper solicitacaoMapper;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              ClienteRepository clienteRepository,
                              CategoriaProjetoRepository categoriaProjetoRepository,
                              IdiomaRepository idiomaRepository,
                              ArquivoStorageService arquivoStorageService,
                              SolicitacaoMapper solicitacaoMapper) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaProjetoRepository = categoriaProjetoRepository;
        this.idiomaRepository = idiomaRepository;
        this.arquivoStorageService = arquivoStorageService;
        this.solicitacaoMapper = solicitacaoMapper;
    }

    @Transactional
    public SolicitacaoResponse criar(SolicitacaoRequest request, List<MultipartFile> arquivos) {
        // RN.1: cada id do formulário precisa existir no banco.
        Cliente cliente = buscar(clienteRepository, request.clienteId(), "Cliente");
        CategoriaProjeto categoria = buscar(categoriaProjetoRepository, request.categoriaProjetoId(), "Categoria do projeto");
        Idioma idiomaOrigem = buscar(idiomaRepository, request.idiomaOrigemId(), "Idioma de origem");
        Idioma idiomaDestino = buscar(idiomaRepository, request.idiomaDestinoId(), "Idioma de destino");

        // RN.2 e CA.4 ficam dentro do construtor da entidade.
        Solicitacao solicitacao = new Solicitacao(cliente, categoria, idiomaOrigem, idiomaDestino, request.descricao());

        // CA.3: grava em disco antes de persistir. Se o banco recusar depois, os arquivos
        // gravados são apagados logo abaixo para não ficarem órfãos.
        List<ArquivoArmazenado> salvos = arquivoStorageService.salvarTodos(arquivos);
        try {
            salvos.forEach(solicitacao::anexarArquivo);
            // saveAndFlush força o INSERT agora: uma violação vira exceção aqui, ainda dentro do try.
            Solicitacao persistida = solicitacaoRepository.saveAndFlush(solicitacao);
            return solicitacaoMapper.paraResponse(persistida);
        } catch (RuntimeException ex) {
            arquivoStorageService.remover(salvos.stream().map(ArquivoArmazenado::caminho).toList());
            throw ex;
        }
    }

    public Page<SolicitacaoResponse> listar(Pageable paginacao) {
        return solicitacaoRepository.findAll(paginacao).map(solicitacaoMapper::paraResponse);
    }

    public SolicitacaoResponse buscarPorId(UUID id) {
        return solicitacaoMapper.paraResponse(buscarEntidade(id));
    }

    // O arquivo é procurado dentro da própria solicitação: um id de arquivo de outra
    // solicitação não é baixado nem por engano nem de propósito.
    public ArquivoParaDownload baixarArquivo(UUID solicitacaoId, UUID arquivoId) {
        ArquivoSolicitacao arquivo = buscarEntidade(solicitacaoId).getArquivos().stream()
                .filter(a -> a.getId().equals(arquivoId))
                .findFirst()
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(arquivoId));

        Resource recurso = arquivoStorageService.carregar(arquivo.getCaminho());
        return new ArquivoParaDownload(recurso, arquivo.getNomeOriginal(), arquivo.getTipoConteudo(), arquivo.getTamanhoBytes());
    }

    private Solicitacao buscarEntidade(UUID id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
    }

    // Um método só para os quatro vínculos: a mensagem de erro diz qual deles falhou.
    private static <T> T buscar(JpaRepository<T, UUID> repositorio, UUID id, String nomeEntidade) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ReferenciaInvalidaException(nomeEntidade, id));
    }

    public record ArquivoParaDownload(Resource recurso, String nomeOriginal, String tipoConteudo, long tamanhoBytes) {
    }
}
