package com.stentio.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

// "message" é sempre texto; "erros" só aparece em falhas de validação de campos.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<CampoErro> erros
) {

    public static ErroResponse de(HttpStatus status, String mensagem) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, null);
    }

    public static ErroResponse deValidacao(String mensagem, List<CampoErro> erros) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, erros);
    }

    // "campo" usa o formato do react-hook-form (precos.1.valor), para o front marcar a linha exata.
    public record CampoErro(String campo, String mensagem) {
    }
}
