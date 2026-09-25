package com.stentio.core.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfOuCnpjValidatorTest {

    private CpfOuCnpjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfOuCnpjValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224725",
            "529.982.247-25",
            "07012356081",
            "070.123.560-81"
    })
    @DisplayName("Deve validar CPFs válidos formatados e não formatados")
    void deveValidarCpfsValidos(String cpf) {
        assertTrue(validator.isValid(cpf, null));
        assertTrue(CpfCnpjUtils.ehValido(cpf));
        assertTrue(CpfCnpjUtils.ehCpfValido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "11.222.333/0001-81",
            "00000000000191",
            "00.000.000/0001-91"
    })
    @DisplayName("Deve validar CNPJs válidos formatados e não formatados")
    void deveValidarCnpjsValidos(String cnpj) {
        assertTrue(validator.isValid(cnpj, null));
        assertTrue(CpfCnpjUtils.ehValido(cnpj));
        assertTrue(CpfCnpjUtils.ehCnpjValido(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000",
            "11111111111",
            "22222222222",
            "12345678901",
            "52998224726", // dígito verificador incorreto
            "07012356066", // dígito verificador incorreto
            "12345"
    })
    @DisplayName("Deve rejeitar CPFs inválidos ou com dígitos repetidos")
    void deveRejeitarCpfsInvalidos(String cpf) {
        assertFalse(validator.isValid(cpf, null));
        assertFalse(CpfCnpjUtils.ehCpfValido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000000",
            "11111111111111",
            "11222333000182", // dígito verificador incorreto
            "12345678901234"
    })
    @DisplayName("Deve rejeitar CNPJs inválidos")
    void deveRejeitarCnpjsInvalidos(String cnpj) {
        assertFalse(validator.isValid(cnpj, null));
        assertFalse(CpfCnpjUtils.ehCnpjValido(cnpj));
    }

    @Test
    @DisplayName("Deve considerar nulo ou em branco como válido para deixar a cargo do @NotBlank")
    void devePermitirNuloOuEmBranco() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }
}
