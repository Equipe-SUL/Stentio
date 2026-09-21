package com.example.stentio.core.service;

import com.example.stentio.core.dto.RecursoFiltro;
import com.example.stentio.core.dto.RecursoRequest;
import com.example.stentio.core.dto.RecursoRequest.PrecoRequest;
import com.example.stentio.core.dto.RecursoResponse;
import com.example.stentio.core.exception.EmailJaCadastradoException;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.mapper.RecursoMapper;
import com.example.stentio.core.model.Idioma;
import com.example.stentio.core.model.Recurso;
import com.example.stentio.core.model.TipoServico;
import com.example.stentio.core.repository.IdiomaRepository;
import com.example.stentio.core.repository.RecursoRepository;
import com.example.stentio.core.repository.RecursoSpecifications;
import com.example.stentio.core.repository.TipoServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Leitura por padrão; métodos que alteram dados sobrescrevem com @Transactional de escrita.
@Service
@Transactional(readOnly = true)
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final IdiomaRepository idiomaRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final RecursoMapper recursoMapper;

    public RecursoService(RecursoRepository recursoRepository,
                          IdiomaRepository idiomaRepository,
                          TipoServicoRepository tipoServicoRepository,
                          RecursoMapper recursoMapper) {
        this.recursoRepository = recursoRepository;
        this.idiomaRepository = idiomaRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.recursoMapper = recursoMapper;
    }

    @Transactional
    public RecursoResponse criar(RecursoRequest request) {
        if (recursoRepository.existsByEmail(Recurso.normalizarEmail(request.email()))) {
            throw new EmailJaCadastradoException();
        }

        Recurso recurso = new Recurso(request.nome(), request.email(), request.telefone());
        aplicarTiposServicoEPrecos(recurso, request);

        // saveAndFlush força o INSERT agora: uma violação de constraint vira exceção aqui, não depois do retorno.
        return recursoMapper.paraResponse(recursoRepository.saveAndFlush(recurso));
    }

    @Transactional
    public RecursoResponse editar(UUID id, RecursoRequest request) {
        Recurso recurso = buscarEntidade(id);

        if (recursoRepository.existsByEmailAndIdNot(Recurso.normalizarEmail(request.email()), id)) {
            throw new EmailJaCadastradoException();
        }

        recurso.atualizarDados(request.nome(), request.email(), request.telefone());
        aplicarTiposServicoEPrecos(recurso, request);
        recursoRepository.flush();

        return recursoMapper.paraResponse(recurso);
    }

    @Transactional
    public RecursoResponse alterarStatus(UUID id, boolean ativo) {
        Recurso recurso = buscarEntidade(id);

        if (ativo) {
            recurso.ativar();
        } else {
            recurso.desativar();
        }

        return recursoMapper.paraResponse(recurso);
    }

    public RecursoResponse buscarPorId(UUID id) {
        return recursoMapper.paraResponse(buscarEntidade(id));
    }

    public Page<RecursoResponse> listar(RecursoFiltro filtro, Pageable paginacao) {
        return recursoRepository.findAll(RecursoSpecifications.filtrar(filtro), paginacao)
                .map(recursoMapper::paraResponse);
    }

    private Recurso buscarEntidade(UUID id) {
        return recursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    private void aplicarTiposServicoEPrecos(Recurso recurso, RecursoRequest request) {
        Map<UUID, TipoServico> tipos = buscarPorIds(
                request.tiposServicoIds(), tipoServicoRepository, TipoServico::getId, "Tipo de serviço");
        recurso.definirTiposServico(tipos.values());

        Map<UUID, Idioma> idiomas = buscarPorIds(
                idsDeIdiomas(request.precos()), idiomaRepository, Idioma::getId, "Idioma");
        recurso.definirPrecos(request.precos().stream()
                .map(preco -> new Recurso.NovoPreco(
                        idiomas.get(preco.idiomaOrigemId()),
                        idiomas.get(preco.idiomaDestinoId()),
                        preco.unidade(),
                        preco.valor()))
                .toList());
    }

    private static Set<UUID> idsDeIdiomas(List<PrecoRequest> precos) {
        return precos.stream()
                .flatMap(preco -> Stream.of(preco.idiomaOrigemId(), preco.idiomaDestinoId()))
                .collect(Collectors.toSet());
    }

    // Uma única consulta por tipo de entidade, em vez de um findById por item da lista.
    private static <T> Map<UUID, T> buscarPorIds(Collection<UUID> ids,
                                                 JpaRepository<T, UUID> repositorio,
                                                 Function<T, UUID> extrairId,
                                                 String nomeEntidade) {
        Set<UUID> idsUnicos = new HashSet<>(ids);
        Map<UUID, T> encontrados = repositorio.findAllById(idsUnicos).stream()
                .collect(Collectors.toMap(extrairId, Function.identity()));

        idsUnicos.removeAll(encontrados.keySet());
        if (!idsUnicos.isEmpty()) {
            throw new RecursoInvalidoException(nomeEntidade + " não encontrado: " + idsUnicos);
        }
        return encontrados;
    }
}
