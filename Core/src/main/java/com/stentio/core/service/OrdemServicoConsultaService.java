package com.stentio.core.service;

import com.stentio.core.dto.OrdemServicoDetalheResponse;
import com.stentio.core.dto.OrdemServicoPaginaResponse;
import com.stentio.core.exception.OrdemServicoInvalidaException;
import com.stentio.core.exception.OrdemServicoNaoEncontradaException;
import com.stentio.core.mapper.OrdemServicoMapper;
import com.stentio.core.model.OrdemServico;
import com.stentio.core.model.StatusOrdemServico;
import com.stentio.core.repository.OrdemServicoRepository;
import com.stentio.core.repository.OrdemServicoSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serviço separado do OrdemServicoService (US-16) de propósito: aquele cria ordens
 * dentro de um lock; este apenas lê. Manter os dois apartados deixa claro o que é
 * leitura e evita que a consulta carregue as dependências da criação.
 */
@Service
@Transactional(readOnly = true)
public class OrdemServicoConsultaService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final String MENSAGEM_PAGINA = "A página deve ser maior ou igual a 1";
    private static final String MENSAGEM_TAMANHO_PAGINA =
            "O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA;
    // Mais recentes primeiro: é a ordem útil para quem acompanha a operação.
    private static final Sort ORDENACAO_PADRAO = Sort.by(Sort.Direction.DESC, "criadoEm");

    private final OrdemServicoRepository ordemServicoRepository;
    private final OrdemServicoMapper ordemServicoMapper;

    public OrdemServicoConsultaService(OrdemServicoRepository ordemServicoRepository,
                                       OrdemServicoMapper ordemServicoMapper) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.ordemServicoMapper = ordemServicoMapper;
    }

    public OrdemServicoPaginaResponse listar(StatusOrdemServico status, UUID clienteId,
                                             int pagina, int tamanhoPagina) {
        if (pagina < 1) {
            throw new OrdemServicoInvalidaException(MENSAGEM_PAGINA);
        }
        if (tamanhoPagina < 1 || tamanhoPagina > TAMANHO_MAXIMO_PAGINA) {
            throw new OrdemServicoInvalidaException(MENSAGEM_TAMANHO_PAGINA);
        }

        PageRequest paginacao = PageRequest.of(pagina - 1, tamanhoPagina, ORDENACAO_PADRAO);
        return OrdemServicoPaginaResponse.de(
                ordemServicoRepository.findAll(OrdemServicoSpecifications.comFiltros(status, clienteId), paginacao)
                        .map(ordemServicoMapper::paraResumo));
    }

    public OrdemServicoDetalheResponse buscarPorId(UUID id) {
        OrdemServico ordem = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        // A conversão acontece aqui dentro: com open-in-view desligado, a sessão fecha
        // ao sair do service e as coleções LAZY não carregariam no controller.
        return ordemServicoMapper.paraDetalhe(ordem);
    }
}
