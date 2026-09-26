package com.stentio.core.service;

import com.stentio.core.dto.CategoriaProjetoRequest;
import com.stentio.core.dto.CategoriaProjetoResponse;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.model.CategoriaProjeto;
import com.stentio.core.repository.CategoriaProjetoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoriaProjetoService {

    private final CategoriaProjetoRepository categoriaProjetoRepository;

    public CategoriaProjetoService(CategoriaProjetoRepository categoriaProjetoRepository) {
        this.categoriaProjetoRepository = categoriaProjetoRepository;
    }

    // CA.4 - Listagem
    public Page<CategoriaProjetoResponse> listar(String nome, Pageable pageable) {
        Page<CategoriaProjeto> pagina;

        if (nome == null || nome.isBlank()) {
            pagina = categoriaProjetoRepository.findAll(pageable);
        } else {
            pagina = categoriaProjetoRepository.findByNomeContainingIgnoreCase(nome, pageable);
        }

        return pagina.map(CategoriaProjetoResponse::new);
    }

    // CA.2 - Criação
    @Transactional
    public CategoriaProjetoResponse criar(CategoriaProjetoRequest request) {
        if (categoriaProjetoRepository.existsByNome(request.nome())) {
            throw new RecursoInvalidoException("Já existe uma categoria de projeto com esse nome.");
        }

        CategoriaProjeto categoriaProjeto = new CategoriaProjeto();
        categoriaProjeto.setNome(request.nome());

        CategoriaProjeto salvo = categoriaProjetoRepository.save(categoriaProjeto);
        return new CategoriaProjetoResponse(salvo);
    }

    // CA.3 - Edição
    @Transactional
    public CategoriaProjetoResponse editar(UUID id, CategoriaProjetoRequest request) {
        Optional<CategoriaProjeto> caixa = categoriaProjetoRepository.findById(id);
        if (caixa.isEmpty()) {
            throw new RecursoNaoEncontradoException(id);
        }

        if (categoriaProjetoRepository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new RecursoInvalidoException("Já existe outra categoria de projeto com esse nome.");
        }

        CategoriaProjeto categoriaProjeto = caixa.get();
        categoriaProjeto.setNome(request.nome());

        CategoriaProjeto salvo = categoriaProjetoRepository.save(categoriaProjeto);
        return new CategoriaProjetoResponse(salvo);
    }

    // CA.2 - Remoção real
    @Transactional
    public void deletar(UUID id) {
        // Correção do erro de compilação: Usando CategoriaProjeto em vez de Idioma
        Optional<CategoriaProjeto> caixa = categoriaProjetoRepository.findById(id);

        if (caixa.isEmpty()) {
            throw new RecursoNaoEncontradoException(id);
        }

        categoriaProjetoRepository.deleteById(id);
    }
}