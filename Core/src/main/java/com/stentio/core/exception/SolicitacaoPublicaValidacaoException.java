package com.stentio.core.exception;

import java.util.List;

public class SolicitacaoPublicaValidacaoException extends RuntimeException {

    private final List<ErroResponse.CampoErro> erros;

    public SolicitacaoPublicaValidacaoException(List<ErroResponse.CampoErro> erros) {
        super("Dados da solicitação pública inválidos");
        this.erros = List.copyOf(erros);
    }

    public List<ErroResponse.CampoErro> erros() {
        return erros;
    }
}
