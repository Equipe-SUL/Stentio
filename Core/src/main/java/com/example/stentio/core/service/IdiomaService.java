package com.example.stentio.core.service;


import com.example.stentio.core.dto.IdiomaRequest;
import com.example.stentio.core.dto.IdiomaResponse;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.model.Idioma;
import com.example.stentio.core.repository.IdiomaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ser.jdk.UUIDSerializer;

import java.util.Optional;
import java.util.UUID;

@Service

public class IdiomaService {
    private final IdiomaRepository idiomaRepository;

    public IdiomaService(IdiomaRepository idiomaRepository) {
        this.idiomaRepository = idiomaRepository;
    }
   @Transactional
    public IdiomaResponse criar(IdiomaRequest request){
        if (idiomaRepository.existsByCodigoIso(request.codigoIso())) {
            throw new RecursoInvalidoException("Já existe um idioma com esse código ISO");
        }

        //  DTO --> Entidade (Banco)
        Idioma idioma = new Idioma();
        idioma.setNome(request.nome());
        idioma.setCodigoIso(request.codigoIso());


        // salva no banco de dados e guarda o resultado
        Idioma idiomaSalvo = idiomaRepository.save(idioma);

        //  salva no dto repsonse para devolver
        return new IdiomaResponse(idiomaSalvo);

    }

    public Page<IdiomaResponse> listar(String nome, Pageable pageable) {
        Page<Idioma> paginaDeIdiomas;

        if (nome == null || nome.isBlank()) {
            paginaDeIdiomas = idiomaRepository.findAll(pageable);
        }
        //  faz a busca filtrada
        else {
            paginaDeIdiomas = idiomaRepository.findByNomeContainingIgnoreCase(nome, pageable);
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
        //evita duplicidade
        if (idiomaRepository.existsByCodigoIsoAndIdNot(request.codigoIso(), id)) {
            throw new RecursoInvalidoException("Já existe um idioma com esse código ISO");
        }


        idioma.setNome(request.nome());
        idioma.setCodigoIso(request.codigoIso());
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
