package com.example.stentio.core.service;

import com.example.stentio.core.dto.TabelaPrecoRequest;
import com.example.stentio.core.dto.TabelaPrecoResponse;
import com.example.stentio.core.exception.RecursoInvalidoException;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.exception.TabelaPrecoDuplicadaException;
import com.example.stentio.core.mapper.TabelaPrecoMapper;
import com.example.stentio.core.model.Idioma;
import com.example.stentio.core.model.TabelaPreco;
import com.example.stentio.core.model.TipoServico;
import com.example.stentio.core.repository.IdiomaRepository;
import com.example.stentio.core.repository.TabelaPrecoRepository;
import com.example.stentio.core.repository.TipoServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TabelaPrecoService {

    private final TabelaPrecoRepository tabelaPrecoRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final IdiomaRepository idiomaRepository;
    private final TabelaPrecoMapper mapper;

    public TabelaPrecoService(TabelaPrecoRepository tabelaPrecoRepository,
                              TipoServicoRepository tipoServicoRepository,
                              IdiomaRepository idiomaRepository,
                              TabelaPrecoMapper mapper) {
        this.tabelaPrecoRepository = tabelaPrecoRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.idiomaRepository = idiomaRepository;
        this.mapper = mapper;
    }

    public Page<TabelaPrecoResponse> listar(Pageable pageable) {
        return tabelaPrecoRepository.findAll(pageable).map(mapper::paraResponse);
    }

    public TabelaPrecoResponse buscarPorId(UUID id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public TabelaPrecoResponse criar(TabelaPrecoRequest request) {
        TipoServico tipoServico = buscarTipoServico(request.tipoServicoId());
        Idioma idiomaOrigem = buscarIdioma(request.idiomaOrigemId(), "origem");
        Idioma idiomaDestino = buscarIdioma(request.idiomaDestinoId(), "destino");

        // RN.1 (Cenário 2)
        if (tabelaPrecoRepository.existsByTipoServicoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndUnidade(
                tipoServico.getId(), idiomaOrigem.getId(), idiomaDestino.getId(), request.unidade())) {
            throw new TabelaPrecoDuplicadaException();
        }

        TabelaPreco tabelaPreco = new TabelaPreco(
                tipoServico, idiomaOrigem, idiomaDestino, request.unidade(), request.valorUnitario());

        return mapper.paraResponse(tabelaPrecoRepository.save(tabelaPreco));
    }

    @Transactional
    public TabelaPrecoResponse editar(UUID id, TabelaPrecoRequest request) {
        TabelaPreco tabelaPreco = buscarEntidade(id);
        TipoServico tipoServico = buscarTipoServico(request.tipoServicoId());
        Idioma idiomaOrigem = buscarIdioma(request.idiomaOrigemId(), "origem");
        Idioma idiomaDestino = buscarIdioma(request.idiomaDestinoId(), "destino");

        // RN.1: a combinação nova não pode pertencer a outra entrada
        if (tabelaPrecoRepository.existsByTipoServicoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndUnidadeAndIdNot(
                tipoServico.getId(), idiomaOrigem.getId(), idiomaDestino.getId(), request.unidade(), id)) {
            throw new TabelaPrecoDuplicadaException();
        }

        tabelaPreco.atualizar(tipoServico, idiomaOrigem, idiomaDestino, request.unidade(), request.valorUnitario());

        return mapper.paraResponse(tabelaPrecoRepository.save(tabelaPreco));
    }

    @Transactional
    public void excluir(UUID id) {
        tabelaPrecoRepository.delete(buscarEntidade(id));
    }

    private TabelaPreco buscarEntidade(UUID id) {
        return tabelaPrecoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    private TipoServico buscarTipoServico(UUID id) {
        return tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RecursoInvalidoException("Tipo de serviço não encontrado: " + id));
    }

    private Idioma buscarIdioma(UUID id, String papel) {
        return idiomaRepository.findById(id)
                .orElseThrow(() -> new RecursoInvalidoException("Idioma de " + papel + " não encontrado: " + id));
    }
}
