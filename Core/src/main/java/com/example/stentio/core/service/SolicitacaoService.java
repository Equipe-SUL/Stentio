package com.example.stentio.core.service;

import com.example.stentio.core.dto.SolicitacaoRequest;
import com.example.stentio.core.dto.SolicitacaoResponse;
import com.example.stentio.core.exception.RecursoNaoEncontradoException;
import com.example.stentio.core.model.Idioma;
import com.example.stentio.core.model.Solicitacao;
import com.example.stentio.core.model.StatusSolicitacao;
import com.example.stentio.core.model.TipoServico;
import com.example.stentio.core.repository.IdiomaRepository;
import com.example.stentio.core.repository.SolicitacaoRepository;
import com.example.stentio.core.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final IdiomaRepository idiomaRepository;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              TipoServicoRepository tipoServicoRepository,
                              IdiomaRepository idiomaRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.idiomaRepository = idiomaRepository;
    }

    @Transactional
    public SolicitacaoResponse criar(SolicitacaoRequest request) {

        //valida se os IDs referenciados existem no banco
        TipoServico tipoServico = tipoServicoRepository.findById(request.tipoServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(request.tipoServicoId()));

        Idioma idiomaOrigem = idiomaRepository.findById(request.idiomaOrigemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(request.idiomaOrigemId()));

        Idioma idiomaDestino = idiomaRepository.findById(request.idiomaDestinoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(request.idiomaDestinoId()));


        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setTipoServico(tipoServico);
        solicitacao.setIdiomaOrigem(idiomaOrigem);
        solicitacao.setIdiomaDestino(idiomaDestino);
        solicitacao.setDescricao(request.descricao());

        solicitacao.setStatus(StatusSolicitacao.PENDENTE_ANALISE);
        solicitacao.setDataRecebimento(LocalDateTime.now());


        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        return new SolicitacaoResponse(solicitacaoSalva);
    }
}