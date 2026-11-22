package com.stentio.core.service;


import com.stentio.core.dto.IdiomaRequest;
import com.stentio.core.dto.IdiomaResponse;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.model.Idioma;
import com.stentio.core.repository.IdiomaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service

public class IdiomaService {
    static final String MENSAGEM_NOME_REPETIDO = "Já existe um idioma com esse nome. "
            + "Diferencie as variantes no nome, por exemplo \"Inglês (EUA)\" e \"Inglês (Reino Unido)\"";

    private final IdiomaRepository idiomaRepository;

    public IdiomaService(IdiomaRepository idiomaRepository) {
        this.idiomaRepository = idiomaRepository;
    }
   @Transactional
    public IdiomaResponse criar(IdiomaRequest request){
        String nome = request.nome().trim();
        String codigoIso = request.codigoIso().trim();
        if (idiomaRepository.existsByCodigoIsoIgnoreCase(codigoIso)) {
            throw new RecursoInvalidoException("Já existe um idioma com esse código ISO");
        }
        if (idiomaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RecursoInvalidoException(MENSAGEM_NOME_REPETIDO);
        }

        //  DTO --> Entidade (Banco)
        Idioma idioma = new Idioma();
        idioma.setNome(nome);
        idioma.setCodigoIso(codigoIso);


        // salva no banco de dados e guarda o resultado
        Idioma idiomaSalvo = idiomaRepository.save(idioma);

        //  salva no dto repsonse para devolver
        return new IdiomaResponse(idiomaSalvo);

    }

    public Page<IdiomaResponse> listar(String nome, Boolean ativo, Pageable pageable) {
        Page<Idioma> paginaDeIdiomas;
        boolean filtroNome = nome != null && !nome.isBlank();

        if (filtroNome && ativo != null) {
            paginaDeIdiomas = idiomaRepository.findByNomeContainingIgnoreCaseAndAtivo(nome, ativo, pageable);
        }
        else if (filtroNome) {
            paginaDeIdiomas = idiomaRepository.findByNomeContainingIgnoreCase(nome, pageable);
        }
        else if (ativo != null) {
            paginaDeIdiomas = idiomaRepository.findByAtivo(ativo, pageable);
        }
        else {
            paginaDeIdiomas = idiomaRepository.findAll(pageable);
        }

        //  Converte a página de Idioma para a página de DTOs response
        return paginaDeIdiomas.map(IdiomaResponse::new);
    }
    @Transactional
    public IdiomaResponse editar(UUID id, IdiomaRequest request) {
        Optional<Idioma> caixa = idiomaRepository.findById(id);
        if (caixa.isEmpty()){
            throw new RecursoNaoEncontradoException(id);
        }
        Idioma idioma = caixa.get();
        String nome = request.nome().trim();
        String codigoIso = request.codigoIso().trim();
        //evita duplicidade
        if (idiomaRepository.existsByCodigoIsoIgnoreCaseAndIdNot(codigoIso, id)) {
            throw new RecursoInvalidoException("Já existe um idioma com esse código ISO");
        }
        if (idiomaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RecursoInvalidoException(MENSAGEM_NOME_REPETIDO);
        }

        idioma.setNome(nome);
        idioma.setCodigoIso(codigoIso);
        Idioma idiomaSalvo = idiomaRepository.save(idioma);
        return new IdiomaResponse(idiomaSalvo);

    }

    @Transactional
    public IdiomaResponse alterarStatus(UUID id, boolean ativo) {

        Optional<Idioma> caixa = idiomaRepository.findById(id);
        if (caixa.isEmpty()) {
            throw new RecursoNaoEncontradoException(id);
        }

        Idioma idioma = caixa.get();
        idioma.setAtivo(ativo);

        Idioma idiomaSalvo = idiomaRepository.save(idioma);
        return new IdiomaResponse(idiomaSalvo);
    }
}
