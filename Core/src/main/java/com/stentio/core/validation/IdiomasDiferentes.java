package com.stentio.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = IdiomasDiferentesValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdiomasDiferentes {

    String message() default "Os idiomas de origem e destino devem ser diferentes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
