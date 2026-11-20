package com.stentio.core.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void deveSerializarErrosDeReferenciaDaSolicitacaoPublicaComoErrosDeCampo() {
        var erro = new SolicitacaoPublicaValidacaoException(List.of(
                new ErroResponse.CampoErro("idiomaDestinoId", "Idioma de destino está inativo")));

        var resposta = new GlobalExceptionHandler().handleSolicitacaoPublicaValidacao(erro);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("idiomaDestinoId", resposta.getBody().erros().getFirst().campo());
        assertEquals("Idioma de destino está inativo", resposta.getBody().erros().getFirst().mensagem());
    }
}
