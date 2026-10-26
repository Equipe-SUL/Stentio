package com.stentio.core.service;

import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import com.stentio.core.model.StatusOrcamento;
import java.time.Instant;

public final class ValidadeLinkOrcamento {
    private ValidadeLinkOrcamento() { }

    public static void validar(StatusOrcamento status, Instant expiracao, Instant utilizadoEm, Instant agora) {
        if (status != StatusOrcamento.AGUARDANDO_APROVACAO || utilizadoEm != null
                || expiracao == null || !expiracao.isAfter(agora)) {
            throw new LinkOrcamentoInvalidoException();
        }
    }
}
