package com.stentio.core.service;

import com.stentio.core.dto.RecursoDisponivelResponse;
import com.stentio.core.dto.RecursoFiltro;
import com.stentio.core.dto.RecursoRequest;
import com.stentio.core.dto.RecursoRequest.PrecoRequest;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.exception.EmailJaCadastradoException;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import com.stentio.core.repository.RecursoSpecifications;
import com.stentio.core.repository.TipoServicoRepository;
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
    /**
     * US-023 CA.2 e CA.3: o próprio profissional cria o seu perfil, já autenticado pela
     * conta que acabou de abrir.
     *
     * <p>As competências passam pelas mesmas regras da US-004 (RN.2) — reaproveitar
     * {@code aplicarTiposServicoEPrecos} não é economia de linhas, é garantia de que não
     * existem dois conjuntos de regras para o mesmo cadastro.
     */
    public RecursoResponse autoCadastrar(RecursoRequest request, UsuarioAutenticado ator) {
        // RN.3: quem entrou com token anterior a esta US não tem id. Pedir login de novo é
        // melhor que criar um perfil órfão, que ninguém conseguiria reivindicar depois.
        if (!ator.temUsuarioId()) {
            throw new AutoCadastroInvalidoException(
                    "Sessão sem identificação de usuário. Entre novamente para concluir o cadastro");
        }

        // RN.1: o perfil é do dono da conta. Sem isto, um recurso autenticado cadastraria
        // um perfil no e-mail de outra pessoa.
        String emailDaConta = Recurso.normalizarEmail(ator.email());
        if (!emailDaConta.equals(Recurso.normalizarEmail(request.email()))) {
            throw new AutoCadastroInvalidoException(
                    "O e-mail do perfil deve ser o mesmo da conta de acesso: " + emailDaConta);
        }

        if (recursoRepository.existsByUsuarioId(ator.usuarioId())) {
            throw new AutoCadastroInvalidoException("Esta conta já tem um perfil de recurso cadastrado");
        }

        if (recursoRepository.existsByEmail(emailDaConta)) {
            throw new EmailJaCadastradoException();
        }

        Recurso recurso = new Recurso(request.nome(), request.email(), request.telefone());
        recurso.vincularUsuario(ator.usuarioId());
        aplicarTiposServicoEPrecos(recurso, request);

        // CA.3: nasce ativo e não verificado. É um pedido de entrada, não uma aprovação.
        return recursoMapper.paraResponse(recursoRepository.saveAndFlush(recurso));
    }

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

    /**
     * US-012 CA.2: profissionais ativos aptos ao par de idiomas e ao tipo de serviço.
     *
     * <p>Os três vínculos são conferidos antes da consulta. Sem isso, um id errado vindo da
     * tela devolveria lista vazia, que o gestor leria como "não há ninguém disponível" —
     * o erro mais caro aqui é o silencioso.
     */
    public List<RecursoDisponivelResponse> listarDisponiveis(UUID idiomaOrigemId,
                                                             UUID idiomaDestinoId,
                                                             UUID tipoServicoId) {
        if (idiomaOrigemId.equals(idiomaDestinoId)) {
            throw new AlocacaoInvalidaException("Os idiomas de origem e destino não devem ser iguais");
        }
        exigirExistencia(idiomaRepository, idiomaOrigemId, "Idioma de origem");
        exigirExistencia(idiomaRepository, idiomaDestinoId, "Idioma de destino");
        exigirExistencia(tipoServicoRepository, tipoServicoId, "Tipo de serviço");

        return recursoRepository.buscarDisponiveis(idiomaOrigemId, idiomaDestinoId, tipoServicoId).stream()
                .map(recurso -> recursoMapper.paraDisponivel(recurso, idiomaOrigemId, idiomaDestinoId))
                .toList();
    }

    private static void exigirExistencia(JpaRepository<?, UUID> repositorio, UUID id, String rotulo) {
        if (!repositorio.existsById(id)) {
            throw new AlocacaoInvalidaException(rotulo + " não encontrado: " + id);
        }
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
