package com.stentio.core.service;

import com.stentio.core.dto.DemandaRecursoResponse;
import com.stentio.core.dto.RecursoResponse;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.mapper.RecursoMapper;
import com.stentio.core.model.ArquivoSolicitacao;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.model.Solicitacao;
import com.stentio.core.model.StatusEtapaOrdemServico;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.support.CodigoVisual;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * US-023: o portal do profissional.
 *
 * <p>Toda consulta parte do {@code usuarioId} do token, nunca de um id que venha na
 * requisição. É isso que faz a CA.7 valer sem depender de o front se comportar: não
 * existe parâmetro para trocar e ver a demanda de outra pessoa.
 */
@Service
public class PortalRecursoService {

    private final RecursoAutenticadoService recursoAutenticado;
    private final EtapaOrdemServicoRepository etapas;
    private final RecursoMapper recursoMapper;
    private final EtapaLiberacaoService etapaLiberacao;

    public PortalRecursoService(RecursoAutenticadoService recursoAutenticado,
                                EtapaOrdemServicoRepository etapas,
                                RecursoMapper recursoMapper,
                                EtapaLiberacaoService etapaLiberacao) {
        this.recursoAutenticado = recursoAutenticado;
        this.etapas = etapas;
        this.recursoMapper = recursoMapper;
        this.etapaLiberacao = etapaLiberacao;
    }

    // CA.5: "Meu perfil"
    @Transactional(readOnly = true)
    public RecursoResponse meuPerfil(UsuarioAutenticado ator) {
        return recursoMapper.paraResponse(recursoAutenticado.resolver(ator));
    }

    /**
     * US-023 CA.5 e US-013 CA.4: o que está com o profissional e o que ele pode pegar.
     *
     * <p>Uma lista só, porque o status já separa as duas coisas sem ambiguidade: DISPARADA
     * é convite aberto — nunca tem dono —, e ALOCADO, ACEITA ou ENTREGUE só aparecem aqui
     * quando são desta pessoa. Um campo "minha" ao lado do status seria o mesmo dado
     * escrito duas vezes, com uma chance a mais de divergirem.
     *
     * <p>CA.7 continua valendo: as duas consultas partem do recurso do token.
     */
    @Transactional(readOnly = true)
    public List<DemandaRecursoResponse> minhasDemandas(UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        UUID recursoId = recurso.getId();

        return Stream.concat(
                        etapas.listarPorRecurso(recursoId).stream(),
                        etapas.listarDisponiveisPara(recursoId, StatusEtapaOrdemServico.DISPARADA).stream())
                .map(etapa -> paraDemanda(etapa, recurso))
                .toList();
    }

    /**
     * O anexo do cliente para o profissional baixar. Mesmas três conferências da lista: a
     * etapa existe, é deste profissional, e o arquivo é da solicitação desta etapa. Qualquer
     * uma que falhe vira 404 — dizer "existe, mas não é seu" já revelaria o arquivo.
     */
    @Transactional(readOnly = true)
    public AnexoParaDownload localizarAnexo(UUID etapaId, UUID arquivoId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .filter(candidata -> candidata.pertenceA(recurso))
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        ArquivoSolicitacao arquivo = etapa.getOrdemServico().getSolicitacao().getArquivos().stream()
                .filter(candidato -> candidato.getId().equals(arquivoId))
                .findFirst()
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        return new AnexoParaDownload(arquivo.getNomeOriginal(), arquivo.getCaminho());
    }

    private DemandaRecursoResponse paraDemanda(EtapaOrdemServico etapa, Recurso recurso) {
        Solicitacao solicitacao = etapa.getOrdemServico().getSolicitacao();
        // Convite aberto não tem dono: o documento só vai para quem assumiu a etapa.
        List<DemandaRecursoResponse.ArquivoDaSolicitacao> arquivos = etapa.pertenceA(recurso)
                ? solicitacao.getArquivos().stream()
                        .map(arquivo -> new DemandaRecursoResponse.ArquivoDaSolicitacao(
                                arquivo.getId(),
                                arquivo.getNomeOriginal(),
                                arquivo.getTamanhoBytes(),
                                "/api/v1/recurso/etapas/" + etapa.getId() + "/arquivos/" + arquivo.getId()))
                        .toList()
                : List.of();

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
                etapaLiberacao.estaLiberada(etapa),
                etapa.getPrazoEstimadoHoras(),
                etapa.getDataLimiteEntrega(),
                arquivos);
    }

    public record AnexoParaDownload(String nomeOriginal, String caminho) {
    }
}
