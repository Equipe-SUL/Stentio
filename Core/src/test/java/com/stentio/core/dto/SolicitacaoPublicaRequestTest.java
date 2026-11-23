package com.stentio.core.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitacaoPublicaRequestTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void aceitaDadosValidosComCpfEIdiomasDiferentes() {
        var violacoes = validator.validate(requisicao(
                "529.982.247-25", "cliente@exemplo.com", UUID.randomUUID(), UUID.randomUUID(), "x".repeat(2000)));

        assertTrue(violacoes.isEmpty());
    }

    @Test
    void exigeDadosCadastraisEReferenciasObrigatorias() {
        var violacoes = validator.validate(new SolicitacaoPublicaRequest(
                null, null, null, null, null, null, null, null, null));

        assertEquals(Set.of(
                        "nomeEmpresa",
                        "nomeRepresentante",
                        "emailRepresentante",
                        "cpfCnpj",
                        "categoriaProjetoId",
                        "idiomaOrigemId",
                        "idiomaDestinoId"),
                campos(violacoes));
    }

    @Test
    void rejeitaEmailEcpfInvalidos() {
        var violacoes = validator.validate(requisicao(
                "111.111.111-11", "email-invalido", UUID.randomUUID(), UUID.randomUUID(), null));

        assertEquals(Set.of("emailRepresentante", "cpfCnpj"), campos(violacoes));
    }

    @Test
    void rejeitaIdiomasIguaisInformandoCampoDeDestino() {
        UUID idioma = UUID.randomUUID();

        var violacoes = validator.validate(requisicao(
                "52998224725", "cliente@exemplo.com", idioma, idioma, null));

        assertEquals(Set.of("idiomaDestinoId"), campos(violacoes));
    }

    @Test
    void limitaDescricaoA2000Caracteres() {
        var violacoes = validator.validate(requisicao(
                "52998224725", "cliente@exemplo.com", UUID.randomUUID(), UUID.randomUUID(), "x".repeat(2001)));

        assertEquals(Set.of("descricao"), campos(violacoes));
    }

    private static SolicitacaoPublicaRequest requisicao(
            String cpfCnpj, String email, UUID idiomaOrigemId, UUID idiomaDestinoId, String descricao) {
        return new SolicitacaoPublicaRequest(
                "Empresa Exemplo",
                "Pessoa Representante",
                email,
                "(11) 99999-1234",
                cpfCnpj,
                UUID.randomUUID(),
                idiomaOrigemId,
                idiomaDestinoId,
                descricao
        );
    }

    private static Set<String> campos(Set<ConstraintViolation<SolicitacaoPublicaRequest>> violacoes) {
        return violacoes.stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
