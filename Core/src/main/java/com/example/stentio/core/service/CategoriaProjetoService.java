package com.example.stentio.core.service;

import com.example.stentio.core.dto.CategoriaProjetoRequest;
import com.example.stentio.core.dto.CategoriaProjetoResponse;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.model.CategoriaProjeto;
import com.example.stentio.core.repository.CategoriaProjetoRepository;
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