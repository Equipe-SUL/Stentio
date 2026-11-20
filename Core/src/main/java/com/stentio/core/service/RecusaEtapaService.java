package com.stentio.core.service;

import com.stentio.core.dto.EtapaStatusResponse;
import com.stentio.core.exception.AlocacaoInvalidaException;
import com.stentio.core.exception.EtapaNaoEncontradaException;
import com.stentio.core.model.EtapaOrdemServico;
import com.stentio.core.model.Recurso;
import com.stentio.core.repository.EtapaOrdemServicoRepository;
import com.stentio.core.support.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * US-013 CA.6 e RN.5: o profissional recusa a alocação direta.
 *
 * <p>Sem UPDATE condicional aqui: a etapa alocada tem um dono só, e não existe uma
 * segunda pessoa disputando a recusa. A guarda de estado mora na entidade.
 */
@Service
public class RecusaEtapaService {

    private final EtapaOrdemServicoRepository etapas;
    private final RecursoAutenticadoService recursoAutenticado;
    private final RecusaEtapaEmailService avisos;
    private final Clock clock;

    public RecusaEtapaService(EtapaOrdemServicoRepository etapas,
                              RecursoAutenticadoService recursoAutenticado,
                              RecusaEtapaEmailService avisos,
                              Clock clock) {
        this.etapas = etapas;
        this.recursoAutenticado = recursoAutenticado;
        this.avisos = avisos;
        this.clock = clock;
    }

    @Transactional
    public EtapaStatusResponse recusar(UUID etapaId, UsuarioAutenticado ator) {
        Recurso recurso = recursoAutenticado.resolver(ator);
        EtapaOrdemServico etapa = etapas.buscarParaAceite(etapaId)
                .orElseThrow(() -> new EtapaNaoEncontradaException(etapaId));

        if (!etapa.pertenceA(recurso)) {
            throw new AlocacaoInvalidaException("Esta etapa não está alocada a você");
        }

        // RN.5: volta a PENDENTE, sem recurso e sem prazo — livre para outra alocação ou
        // para o disparo aberto. A entidade recusa qualquer estado que não seja ALOCADO,
        // então uma etapa já aceita não passa daqui.
        etapa.recusar();
        etapas.save(etapa);

        // CA.6: na mesma transação. Equipe avisada só se a recusa valeu.
        avisos.avisarRecusa(etapa, recurso, clock.instant());

        return new EtapaStatusResponse(etapa.getId(), etapa.getStatus());
    }
}
