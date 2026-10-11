package com.stentio.core.service;

import com.stentio.core.dto.OrcamentoDecisaoResponse;
import com.stentio.core.dto.OrcamentoPublicoResponse;
import com.stentio.core.event.OrcamentoDecisaoEvento;
import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.*;
import com.stentio.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrcamentoPublicoService {
    private final OrcamentoRepository orcamentos;
    private final OrcamentoHistoricoRepository historico;
    private final OrcamentoDecisaoOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrcamentoPublicoService(OrcamentoRepository orcamentos, OrcamentoHistoricoRepository historico,
                                  OrcamentoDecisaoOutboxRepository outbox, ObjectMapper objectMapper, Clock clock) {
        this.orcamentos = orcamentos;
        this.historico = historico;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OrcamentoPublicoResponse consultar(String token) {
        Orcamento orcamento = orcamentos.findByTokenHash(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        validar(orcamento, clock.instant());
        // Materializa os itens LAZY dentro da transação; não devolve entidade JPA.
        return OrcamentoPublicoResponse.de(orcamento);
    }

    @Transactional
    public OrcamentoDecisaoResponse aprovar(String token) {
        return decidir(token, StatusOrcamento.APROVADO, null);
    }

    @Transactional
    public OrcamentoDecisaoResponse rejeitar(String token, String motivo) {
        // Também valida chamadas internas, além do @Valid do controller.
        if (motivo == null || motivo.isBlank() || motivo.length() > 2000) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório e deve ter no máximo 2000 caracteres");
        }
        return decidir(token, StatusOrcamento.REJEITADO, motivo.strip());
    }

    private OrcamentoDecisaoResponse decidir(String token, StatusOrcamento decisao, String motivo) {
        Orcamento orcamento = orcamentos.buscarPorTokenComLock(TokenOrcamento.hash(token))
                .orElseThrow(LinkOrcamentoInvalidoException::new);
        // Valida a expiração depois de obter o lock, inclusive se houve espera.
        Instant agora = clock.instant();
        validar(orcamento, agora);

        OrcamentoHistorico registro = new OrcamentoHistorico();
        registro.setOrcamento(orcamento);
        registro.setStatusAnterior(orcamento.getStatus());
        registro.setStatusNovo(decisao);
        registro.setMotivo(motivo);
        registro.setDataDecisao(agora);

        orcamento.setStatus(decisao);
        orcamento.setTokenUtilizadoEm(agora);
        orcamentos.save(orcamento);
        historico.save(registro);

        UUID eventoId = UUID.randomUUID();
        OrcamentoDecisaoEvento evento = new OrcamentoDecisaoEvento(eventoId, 1,
                orcamento.getId(), decisao, motivo, agora);
        OrcamentoDecisaoOutbox pendente = new OrcamentoDecisaoOutbox();
        pendente.setId(eventoId);
        pendente.setOrcamentoId(orcamento.getId());
        pendente.setRoutingKey(decisao == StatusOrcamento.APROVADO ? "orcamento.aprovado" : "orcamento.rejeitado");
        pendente.setPayload(objectMapper.writeValueAsString(evento));
        pendente.setCriadoEm(agora);
        pendente.setProximaTentativaEm(agora);
        outbox.save(pendente);

        return new OrcamentoDecisaoResponse(orcamento.getId(), decisao, agora);
    }

    private void validar(Orcamento orcamento, Instant agora) {
        ValidadeLinkOrcamento.validar(orcamento.getStatus(), orcamento.getDataExpiracao(),
                orcamento.getTokenUtilizadoEm(), agora);
    }
}
