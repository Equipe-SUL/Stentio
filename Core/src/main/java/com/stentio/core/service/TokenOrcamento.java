package com.stentio.core.service;

import com.stentio.core.exception.LinkOrcamentoInvalidoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TokenOrcamento {
    private static final Pattern UUID_CANONICO = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private TokenOrcamento() { }

    public static UUID gerar() {
        return UUID.randomUUID();
    }

    public static String hash(String token) {
        if (token == null || !UUID_CANONICO.matcher(token).matches()) {
            throw new LinkOrcamentoInvalidoException();
        }
        // UUID.fromString sozinho aceita formatos abreviados; validamos antes.
        String canonico = UUID.fromString(token).toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonico.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
