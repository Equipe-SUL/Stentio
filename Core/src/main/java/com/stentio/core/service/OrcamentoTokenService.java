package com.stentio.core.service;

import com.stentio.core.model.Orcamento;
import com.stentio.core.model.StatusOrcamento;
import com.stentio.core.repository.OrcamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrcamentoTokenService {
    private final OrcamentoRepository repository;
    private final Clock clock;

    public OrcamentoTokenService(OrcamentoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // Integração interna para o serviço que prepara/envia o orçamento, sem rota pública de emissão.
    // O chamador deve guardar/enviar o UUID retornado; o banco não permite recuperá-lo.
    @Transactional
    public UUID emitir(UUID orcamentoId, Instant expiracao) {
        Orcamento orcamento = repository.buscarPorIdComLock(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        if (expiracao == null || !expiracao.isAfter(clock.instant())) {
            throw new IllegalArgumentException("Expiração deve ser futura");
        }
        if (orcamento.getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new IllegalArgumentException("Somente orçamento aguardando aprovação pode receber um link");
        }
        UUID token = TokenOrcamento.gerar();
        orcamento.setTokenHash(TokenOrcamento.hash(token.toString()));
        orcamento.setTokenUtilizadoEm(null);
        orcamento.setDataExpiracao(expiracao);
        repository.save(orcamento);
        return token;
    }
}
