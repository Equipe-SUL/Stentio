package com.stentio.core.service;

import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.AutoCadastroInvalidoException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.repository.RecursoRepository;
import com.stentio.core.support.CodigoVisual;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US-023: o portal do profissional.
 *
 * <p>Toda consulta parte do {@code usuarioId} do token, nunca de um id que venha na
 * requisição. É isso que faz a CA.7 valer sem depender de o front se comportar: não
 * existe parâmetro para trocar e ver a demanda de outra pessoa.
 */
@Service
public class PortalRecursoService {

    private final RecursoRepository recursos;
    private final EtapaOrdemServicoRepository etapas;
    private final RecursoMapper recursoMapper;

    public PortalRecursoService(RecursoRepository recursos,
                                EtapaOrdemServicoRepository etapas,
                                RecursoMapper recursoMapper) {
        this.recursos = recursos;
        this.etapas = etapas;
        this.recursoMapper = recursoMapper;
    }

    // CA.5: "Meu perfil"
    @Transactional(readOnly = true)
    public RecursoResponse meuPerfil(UsuarioAutenticado ator) {
        return recursoMapper.paraResponse(meuRecurso(ator));
    }

    // CA.5: "Minhas demandas", somente leitura
    @Transactional(readOnly = true)
    public List<DemandaRecursoResponse> minhasDemandas(UsuarioAutenticado ator) {
        return etapas.listarPorRecurso(meuRecurso(ator).getId()).stream()
                .map(PortalRecursoService::paraDemanda)
                .toList();
    }

    private Recurso meuRecurso(UsuarioAutenticado ator) {
        if (!ator.temUsuarioId()) {
            throw new AutoCadastroInvalidoException(
                    "Sessão sem identificação de usuário. Entre novamente para acessar o portal");
        }
        return recursos.buscarPorUsuario(ator.usuarioId())
                .orElseThrow(() -> new AutoCadastroInvalidoException(
                        "Esta conta ainda não tem perfil de recurso. Conclua o cadastro para ver suas demandas"));
    }

    private static DemandaRecursoResponse paraDemanda(EtapaOrdemServico etapa) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();

        return new DemandaRecursoResponse(
                etapa.getId(),
                etapa.getOrdemServico().getId(),
                CodigoVisual.gerar("OS", etapa.getOrdemServico().getCriadoEm(), etapa.getOrdemServico().getId()),
                etapa.getOrdem(),
                etapa.getNome(),
                etapa.getTipoServico().getNome(),
                solicitacao.getIdiomaOrigem().getNome(),
                solicitacao.getIdiomaDestino().getNome(),
                etapa.getStatus(),
                etapa.getPrazoEstimadoHoras(),
                etapa.getDataLimiteEntrega());
    }
}
