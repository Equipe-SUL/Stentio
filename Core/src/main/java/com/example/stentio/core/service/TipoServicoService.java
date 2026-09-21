package com.example.stentio.core.service;

import com.example.stentio.core.dto.TipoServicoRequest;
import com.example.stentio.core.dto.TipoServicoResponse;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.model.TipoServico;
import com.example.stentio.core.repository.TipoServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TipoServicoService {

    private final TipoServicoRepository tipoServicoRepository;

    public TipoServicoService(TipoServicoRepository tipoServicoRepository) {
        this.tipoServicoRepository = tipoServicoRepository;
    }


    public Page<TipoServicoResponse> listar(String nome, Pageable pageable) {
        Page<TipoServico> pagina;
        if (nome == null || nome.isBlank()) {
            pagina = tipoServicoRepository.findAll(pageable);
        } else {
            pagina = tipoServicoRepository.findByNomeContainingIgnoreCase(nome, pageable);
        }
        return pagina.map(TipoServicoResponse::new);
    }


    @Transactional
    public TipoServicoResponse criar(TipoServicoRequest request) {
        if (tipoServicoRepository.existsByNome(request.nome())) {
            throw new RecursoInvalidoException("Já existe um Tipo de Serviço com este nome.");
        }

        TipoServico tipoServico = new TipoServico();
        tipoServico.setNome(request.nome());
        tipoServico.setAtivo(true); // Nasce ativo por padrão

        TipoServico salvo = tipoServicoRepository.save(tipoServico);
        return new TipoServicoResponse(salvo);
    }


    @Transactional
    public TipoServicoResponse editar(UUID id, TipoServicoRequest request) {
        Optional<TipoServico> caixa = tipoServicoRepository.findById(id);
        if (caixa.isEmpty()) {
            throw new RecursoNaoEncontradoException(id);
        }

        if (tipoServicoRepository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new RecursoInvalidoException("Já existe outro Tipo de Serviço com este nome.");
        }

        TipoServico tipoServico = caixa.get();
        tipoServico.setNome(request.nome());

        TipoServico atualizado = tipoServicoRepository.save(tipoServico);
        return new TipoServicoResponse(atualizado);
    }

    @Transactional
    public TipoServicoResponse alterarStatus(UUID id, boolean ativo) {
        Optional<TipoServico> caixa = tipoServicoRepository.findById(id);
        if (caixa.isEmpty()) {
            throw new RecursoNaoEncontradoException(id);
        }

        TipoServico tipoServico = caixa.get();
        tipoServico.setAtivo(ativo);

        TipoServico salvo = tipoServicoRepository.save(tipoServico);
        return new TipoServicoResponse(salvo);
    }
}