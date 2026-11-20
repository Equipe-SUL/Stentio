package com.stentio.core.validation;

import com.stentio.core.dto.SolicitacaoPublicaRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IdiomasDiferentesValidator implements ConstraintValidator<IdiomasDiferentes, SolicitacaoPublicaRequest> {

    @Override
    public boolean isValid(SolicitacaoPublicaRequest requisicao, ConstraintValidatorContext context) {
        if (requisicao == null
                || requisicao.idiomaOrigemId() == null
                || requisicao.idiomaDestinoId() == null
                || !requisicao.idiomaOrigemId().equals(requisicao.idiomaDestinoId())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("idiomaDestinoId")
                .addConstraintViolation();
        return false;
    }
}
