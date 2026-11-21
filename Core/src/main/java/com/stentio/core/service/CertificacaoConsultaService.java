package com.stentio.core.service;

import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusCertificacao;
import com.stentio.core.repository.RecursoCertificacaoRepository;
import org.springframework.stereotype.Service;

/**
 * US-024 CA.3 e CA.4: este profissional está certificado para o par desta etapa?
 *
 * <p>Um lugar só para a pergunta, porque ela é feita de dois lados — a alocação, onde o
 * gestor escolhe alguém, e o aceite, onde o profissional se oferece. Duas cópias da mesma
 * conferência divergiriam na primeira mudança de regra.
 */
@Service
public class CertificacaoConsultaService {

    private final RecursoCertificacaoRepository certificacoes;

    public CertificacaoConsultaService(RecursoCertificacaoRepository certificacoes) {
        this.certificacoes = certificacoes;
    }

    /** O par vem da solicitação da OS, não da etapa: é lá que ele é declarado. */
    public boolean certificadoParaAEtapa(Recurso recurso, EtapaOrdemServico etapa) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        return certificacoes.existsByRecursoIdAndIdiomaOrigemIdAndIdiomaDestinoIdAndStatus(
                recurso.getId(),
                solicitacao.getIdiomaOrigem().getId(),
                solicitacao.getIdiomaDestino().getId(),
                StatusCertificacao.APROVADA);
    }
}
