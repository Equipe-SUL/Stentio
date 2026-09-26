package com.stentio.core.service;

import com.stentio.core.dto.TabelaPrecoRequest;
import com.stentio.core.dto.TabelaPrecoResponse;
import com.stentio.core.exception.RecursoInvalidoException;
import com.stentio.core.exception.RecursoNaoEncontradoException;
import com.stentio.core.exception.TabelaPrecoDuplicadaException;
import com.stentio.core.mapper.TabelaPrecoMapper;
import com.stentio.core.model.Idioma;
import com.stentio.core.model.TabelaPreco;
import com.stentio.core.model.TipoServico;
import com.stentio.core.repository.IdiomaRepository;
import com.stentio.core.repository.TabelaPrecoRepository;
import com.stentio.core.repository.TipoServicoRepository;
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
