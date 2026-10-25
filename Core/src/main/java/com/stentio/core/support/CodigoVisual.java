package com.stentio.core.support;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

/**
 * Código legível de exibição (ex.: OS-2026-A1B2C3, ORC-2026-A1B2C3).
 * Derivado de dados imutáveis — ano de criação e id —, então não precisa de coluna no banco
 * nem de contador sob concorrência, e o mesmo registro sempre gera o mesmo código.
 */
public final class CodigoVisual {

    private CodigoVisual() {
    }

    public static String gerar(String prefixo, Instant criadoEm, UUID id) {
        String ano = String.valueOf(criadoEm.atZone(ZoneOffset.UTC).getYear());
        String sufixo = id.toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return prefixo + "-" + ano + "-" + sufixo;
    }
}
