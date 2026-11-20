package com.stentio.core.service;

import com.stentio.core.dto.LogAuditoriaFiltro;
import com.stentio.core.dto.LogAuditoriaPaginaResponse;
import com.stentio.core.exception.AuditoriaFiltroInvalidoException;
import com.stentio.core.repository.LogAuditoriaRepository;
import com.stentio.core.repository.LogAuditoriaSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-019 CA.2 / CA.3: consulta do log de auditoria. Só leitura (RN.1): a gravação é
 * exclusiva do {@link AuditoriaStatusListener}.
 */
@Service
@Transactional(readOnly = true)
public class LogAuditoriaService {

    static final int TAMANHO_MAXIMO_PAGINA = 100;

    // CA.2: mais recente primeiro; o id desempata registros gravados no mesmo instante.
    private static final Sort ORDENACAO = Sort.by(Sort.Order.desc("dataHora"), Sort.Order.desc("id"));

    private final LogAuditoriaRepository logs;

    public LogAuditoriaService(LogAuditoriaRepository logs) {
        this.logs = logs;
    }

    /** "pagina" chega em base 1, como nas demais listagens do Core. */
    public LogAuditoriaPaginaResponse listar(LogAuditoriaFiltro filtro, int pagina, int tamanhoPagina) {
        if (pagina < 1) {
            throw new AuditoriaFiltroInvalidoException("O parâmetro 'page' deve ser maior ou igual a 1");
        }
        if (tamanhoPagina < 1 || tamanhoPagina > TAMANHO_MAXIMO_PAGINA) {
            throw new AuditoriaFiltroInvalidoException(
                    "O parâmetro 'size' deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        if (filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new AuditoriaFiltroInvalidoException("'data_inicio' não pode ser posterior a 'data_fim'");
        }

        PageRequest paginacao = PageRequest.of(pagina - 1, tamanhoPagina, ORDENACAO);
        return LogAuditoriaPaginaResponse.de(logs.findAll(LogAuditoriaSpecifications.comFiltros(filtro), paginacao));
    }
}
