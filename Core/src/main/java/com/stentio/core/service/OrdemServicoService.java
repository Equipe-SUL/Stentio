package com.stentio.core.service;

import com.stentio.core.exception.OrcamentoInvalidoException;
import com.stentio.core.exception.OrcamentoNaoEncontradoException;
import com.stentio.core.model.Orcamento;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.OrcamentoRepository;
import com.stentio.core.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrdemServicoService {
    private final OrcamentoRepository orcamentos;
    private final OrdemServicoRepository ordensServico;

    public OrdemServicoService(OrcamentoRepository orcamentos, OrdemServicoRepository ordensServico) {
        this.orcamentos = orcamentos;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public OrdemServico criarParaOrcamentoAprovado(UUID orcamentoId) {
        if (orcamentoId == null) {
            throw new OrcamentoInvalidoException("O orçamento é obrigatório para criar a ordem de serviço");
        }

        // O mesmo lock da aprovação serializa eventos duplicados, inclusive entre instâncias do Core.
        Orcamento orcamento = orcamentos.buscarPorIdComLock(orcamentoId)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(orcamentoId));
        if (orcamento.getStatus() != StatusOrcamento.APROVADO) {
            throw new OrcamentoInvalidoException("Somente orçamentos aprovados podem gerar ordem de serviço");
        }

        return ordensServico.findByOrcamentoId(orcamentoId).orElseGet(() -> {
            if (orcamento.getSolicitacao() == null) {
                throw new OrcamentoInvalidoException("O orçamento deve estar vinculado a uma solicitação");
            }
            return ordensServico.save(new OrdemServico(orcamento));
        });
    }
}
